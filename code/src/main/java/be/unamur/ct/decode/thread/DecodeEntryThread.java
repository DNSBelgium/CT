package be.unamur.ct.decode.thread;

import be.unamur.ct.decode.service.DecodeService;
import be.unamur.ct.download.model.LogEntry;


/**
 * Thread class to launch the decode process for a downloaded certificate
 *
 * TODO: use ExecutorService and Runnable or Callable or ...
 */
public class DecodeEntryThread extends Thread {

    private DecodeService decodeService;
    private LogEntry entry;


    /**
     * Constructor
     *
     * @author Jules Dejaeghere
     * @param entry         Downloaded certificate in Base64 representation
     * @param decodeService Reference of the DecodeService to use
     * @see LogEntry
     */
    public DecodeEntryThread(LogEntry entry, DecodeService decodeService) {
        this.entry = entry;
        this.decodeService = decodeService;
    }


    /**
     * Starts the decode process for the entry saved in the variables of the instance
     *
     * @author Jules Dejaeghere
     */
    @Override
    public void run() {
        decodeService.decodeToCert(entry);
    }
}
