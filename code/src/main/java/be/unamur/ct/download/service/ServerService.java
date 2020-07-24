package be.unamur.ct.download.service;

import be.unamur.ct.data.dao.ServerDao;
import be.unamur.ct.data.dao.SliceDao;
import be.unamur.ct.decode.service.DecodeService;
import be.unamur.ct.decode.thread.DecodeEntryThread;
import be.unamur.ct.download.model.LogEntry;
import be.unamur.ct.download.model.LogList;
import be.unamur.ct.download.model.Server;
import be.unamur.ct.download.model.Slice;
import be.unamur.ct.download.thread.SearchSliceThread;
import be.unamur.ct.thread.ThreadPool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;


/**
 * Service class providing method to handle Certificate Transparency log servers in the application
 * Log server are where the logs are downloaded from to get certificates
 */
@Service
public class ServerService {

    @Autowired
    private ServerDao serverDao;
    @Autowired
    private SliceDao sliceDao;
    @Autowired
    private DecodeService decodeService;
    @Autowired
    private ThreadPool threadPool;

    private final ExecutorService decoder = ThreadPool.getDecodeExecutor();
    private final static Logger logger = LoggerFactory.getLogger(ServerService.class);


    public ServerService() {}


    /**
     * Updates the slices for the specified server and start downloading logs using the created slices.
     * The slices are handled by different thread in order to reduce waiting time due to blocking calls in the process
     *
     * @author Jules Dejaeghere
     * @param server Server to download logs from
     */
    public void startSearch(Server server) {
        logger.info("Updating slices for " + server.getNickname());
        updateSlices(server);

        logger.info("Adding slices to queue");
        Iterable<Slice> slices = sliceDao.findByServerOrderByStartSlice(server);
        for (Slice s : slices) {
            SearchSliceThread search = new SearchSliceThread(s, this);
            ThreadPool.getSliceExecutor().execute(search);
        }
    }


    /**
     * Downloads logs from the specified slice.
     * Once downloaded each log is sent in a new to thread to be decoded and saved in the database.
     *
     * @author Jules Dejaeghere
     * @param slice Slice to download logs from
     * @see Slice
     */
    public void searchSlice(Slice slice) {
        Server server = slice.getServer();
        long start = slice.getNext();
        long size = slice.getEndSlice();
        long step = 1000;
        boolean interrupted = false;
        logger.info("Starting slice " + slice.toString());

        while (start <= size && !Thread.currentThread().isInterrupted()) {

            long end = Math.min((start + step - 1), size);

            // Create REST request
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(server.getUrl() + "ct/v1/get-entries?start=" + start + "&end=" + end)
                    .build();

            LogList log = null;
            try {
                // Execute request
                Call call = client.newCall(request);
                Response response = call.execute();

                // Save received JSON in an Object using ObjectMapper
                ObjectMapper objectMapper = new ObjectMapper();
                log = objectMapper.readValue(response.body().string(), LogList.class);
            } catch (SocketTimeoutException e) {
                logger.warn("Timeout while requesting data from server");
            } catch (InterruptedIOException e) {
                interrupted = true;
                Thread.currentThread().interrupt();
                logger.warn("Thread interrupted");
            } catch (IOException e) {
                logger.error("Error while requesting logs to server");
            }

            // NEXT STEP - Send logs to be decoded
            if (log != null) {
                for (LogEntry entry : log) {
                    decoder.execute(new DecodeEntryThread(entry, decodeService));
                }
            }
            if (!interrupted) {
                slice.setNext(end + 1);
                sliceDao.save(slice);
                serverDao.save(server);
                start += step;
            }
        }
    }


    /**
     * Creates or updates slices for the given server.  Once created, slices are stored in the database
     *
     * @author Jules Dejaeghere
     * @param server Server to create slices for
     * @see Slice
     */
    public void updateSlices(Server server) {
        /*
         * If no slices exists, create all slices
         * If slices exists, delete used slices except last and create new slices if needed
         */
        long start;
        long end;
        long step = 1000000;

        // Define where to start to create slices
        List<Slice> slices = sliceDao.findByServerOrderByEndSliceDesc(server);
        Slice last = null;
        if (slices.isEmpty()) {
            start = 0;
            logger.info("No slices yet");
        } else {
            last = slices.get(0);
            start = last.getEndSlice() + 1;
            logger.info("Already slices to " + start);
        }

        // Clear all completely used slices
        int count = 0;
        for (Slice s : slices) {
            if (s.getNext() > s.getEndSlice()) {
                sliceDao.deleteById(s.getId());
                count++;
            }
        }
        logger.info(count + " slices deleted");

        // Re-insert last Slice to know where we previously stopped
        if (last != null && !sliceDao.existsById(last.getId())) {
            sliceDao.save(last);
            logger.info("Re-saving last slice");
        }

        long serverSize = checkSize(server);

        while (start < serverSize) {
            end = Math.min(start + step - 1, serverSize - 1);
            sliceDao.save(new Slice(start, end, start, server));
            start += step;
        }

    }


    /**
     * Returns the size (the number of logs it contains) of a Certificate Transparency log server
     *
     * @author Jules Dejaeghere
     * @param server Server to check size for
     * @return Number of logs present in the server at the time of the execution
     */
    public long checkSize(Server server) {

        // Create REST request
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(server.getUrl() + "ct/v1/get-sth")
                .build();

        // Execute request
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            String json = response.body().string();

            // Save received JSON in an Object using ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(json);

            return jsonNode.get("tree_size").asLong();
        } catch (IOException e) {
            return 0;
        }
    }
}
