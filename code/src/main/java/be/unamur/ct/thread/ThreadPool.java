package be.unamur.ct.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Class handling multiples ExecutorServices used in the application.
 * This class uses the Singleton design pattern to prevent multiples instances of the same ExecutorService to be created.
 *
 * TODO:  why use Singleton in a Spring application ??  It is already a spring bean so ... ?
 * TODO:  Thread classes in this project are actually used as a Runnable => better to make them of Runnable type
 * TODO:  Replace with Spring abstraction for executor ??
 *
 * @see ExecutorService
 */
@Component
public class ThreadPool {
    static private ExecutorService serverExecutor = null;
    static private ExecutorService sliceExecutor = null;
    static private ExecutorService decodeExecutor = null;
    static private ExecutorService VATScrapperExecutor = null;

    static private final Logger logger = LoggerFactory.getLogger(ThreadPool.class);

    static private Integer threadsDecode;
    static private Integer threadsSlice;
    static private Integer threadsScrap;


    /**
     * Because the @Value cannot be applied to a static variable, this method set the @Value to the static variable
     *
     * @author Jules Dejaeghere
     * @param value Injected value to set
     */
    @Value("${threads-decode}")
    public void setThreadsDecode(Integer value) {
        threadsDecode = value;
    }


    /**
     * Because the @Value cannot be applied to a static variable, this method set the @Value to the static variable
     *
     * @author Jules Dejaeghere
     * @param value Injected value to set
     */
    @Value("${threads-slice}")
    public void setThreadsSlice(Integer value) {
        threadsSlice = value;
    }


    /**
     * Because the @Value cannot be applied to a static variable, this method set the @Value to the static variable
     *
     * @author Jules Dejaeghere
     * @param value Injected value to set
     */
    @Value("${threads-scrap}")
    public void setThreadsScrap(Integer value) {
        threadsScrap = value;
    }

    /**
     * ExecutorService used to handle threads decoding certificates
     *
     * @author Jules Dejaeghere
     * @return ExecutorService to decode certificates
     */
    static public synchronized ExecutorService getDecodeExecutor() {
        if (decodeExecutor == null) {
            logger.info("Creating " + threadsDecode + " threads for decodeExecutor");
            decodeExecutor = Executors.newFixedThreadPool(threadsDecode);
        }
        return decodeExecutor;
    }


    /**
     * ExecutorService used to handle threads downloading logs from slices
     *
     * @author Jules Dejaeghere
     * @return ExecutorService to download logs form slices
     */
    static public synchronized ExecutorService getSliceExecutor() {
        if (sliceExecutor == null) {
            logger.info("Creating " + threadsSlice + " threads for sliceExecutor");
            sliceExecutor = Executors.newFixedThreadPool(threadsSlice);
        }
        return sliceExecutor;
    }


    /**
     * ExecutorService used to handle threads creating slices from a server and threads resuming VAT scrapping
     *
     * @author Jules Dejaeghere
     * @return ExecutorService to create slices and to resume VAT scrapping
     */
    static public synchronized ExecutorService getServerExecutor() {
        if (serverExecutor == null) {
            serverExecutor = Executors.newSingleThreadExecutor();
        }
        return serverExecutor;
    }


    /**
     * ExecutorService used to handle threads scrapping VAT number from web sites
     *
     * @author Jules Dejaeghere
     * @return ExecutorService to scrap web site for VAT number
     */
    static public synchronized ExecutorService getVATScrapperExecutor() {
        if (VATScrapperExecutor == null) {
            logger.info("Creating " + threadsScrap + " threads for VATScrapperExecutor");
            VATScrapperExecutor = Executors.newFixedThreadPool(threadsScrap);
        }
        return VATScrapperExecutor;
    }
}