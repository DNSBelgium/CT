package be.unamur.ct.web.controller;

import be.unamur.ct.data.dao.CertificateDao;
import be.unamur.ct.data.dao.ServerDao;
import be.unamur.ct.data.service.CertificateService;
import be.unamur.ct.decode.model.Certificate;
import be.unamur.ct.download.model.Server;
import be.unamur.ct.download.service.ServerService;
import be.unamur.ct.download.thread.ScanLogThread;
import be.unamur.ct.scrap.service.VatScrapingService;
import be.unamur.ct.scrap.thread.ResumeVATScrapThread;
import be.unamur.ct.thread.ThreadPool;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class WebController {

    @Autowired
    private ServerDao serverDao;

    @Autowired
    private CertificateDao certificateDao;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private VatScrapingService vatScrapingService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private ThreadPool threadPool;

    private Logger logger = LoggerFactory.getLogger(WebController.class);


    /**
     * Constructs the HTML page for the home web page
     *
     * @author Jules Dejaeghere
     * @return      HTML template to use
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }


    /**
     * Constructs the HTML page for the server list page, displaying all
     * the Certificate Transparency servers known by the application
     *
     * @author Jules Dejaeghere
     * @param model Model to use to create the HTML page
     * @return      HTML template to use
     */
    @GetMapping("/serverList")
    public String showServerList(Model model) {

        Iterable<Server> servers = serverDao.findAll();
        model.addAttribute("servers", servers);

        Server serverFrom = new Server();
        model.addAttribute("serverForm", serverFrom);

        return "serverList";
    }


    /**
     * Handle new server submission via the web page and redirect to the server list page
     *
     * @author Jules Dejaeghere
     * @param model Model to use to create the HTML page
     * @return      Redirection to apply
     */
    @PostMapping("/serverList")
    public String saveServer(Model model, @ModelAttribute("serverForm") Server serverForm) {
        logger.info(serverForm.getNickname() + " " + serverForm.getUrl());

        Iterable<Server> servers = serverDao.findAll();
        model.addAttribute("servers", servers);

        String url = serverForm.getUrl();
        url = url.endsWith("/") ? url : url + "/";
        serverForm.setUrl((url));

        if (serverDao.existsByUrl(url)) {
            logger.warn("URL already exists in database: " + url);
            model.addAttribute("errorMessage", "URL already exists in database");
            return "serverList";
        } else {

            try {
                Server serverAdded = serverDao.save(serverForm);
                if (serverAdded == null) {
                    model.addAttribute("errorMessage", "Cannot add server in the database");
                    return "serverList";
                }
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Cannot add server in the database");
                return "serverList";
            }

        }

        return "redirect:/serverList";
    }


    /**
     * Constructs the HTML page displaying the status of the application
     *
     * @author Jules Dejaeghere
     * @param model Model to use to create the HTML page
     * @return      HTML template to use
     */
    @GetMapping("/status")
    public String status(Model model) {

        model.addAttribute("server",
                threadPool.getServerExecutor().isShutdown() ?
                        (threadPool.getServerExecutor().isTerminated() ? "Closed" : "Closing") : "Running");

        model.addAttribute("slice",
                threadPool.getSliceExecutor().isShutdown() ?
                        (threadPool.getSliceExecutor().isTerminated() ? "Closed" : "Closing") : "Running");

        model.addAttribute("decode",
                threadPool.getDecodeExecutor().isShutdown() ?
                        (threadPool.getDecodeExecutor().isTerminated() ? "Closed" : "Closing") : "Running");

        model.addAttribute("vat",
                threadPool.getVATScrapperExecutor().isShutdown() ?
                        (threadPool.getVATScrapperExecutor().isTerminated() ? "Closed" : "Closing") : "Running");

        return "status";

    }


    /**
     * Stops the serverExecutor and the sliceExecutor in order to stop the downloading activity of the application.
     * Redirects to the status page
     *
     * @author Jules Dejaeghere
     * @return      Redirection to apply
     * @see ThreadPool
     */
    @GetMapping("/stopdown")
    public String stopdown() {
        threadPool.getServerExecutor().shutdown();
        threadPool.getSliceExecutor().shutdownNow();

        return "redirect:/status";
    }


    /**
     * Stops the decodeExecutor and the vatScrapperExecutor in order to stop the processing activity of the application.
     * Redirects to the status page
     *
     * @author Jules Dejaeghere
     * @return      Redirection to apply
     * @see ThreadPool
     */
    @GetMapping("/stopprocess")
    public String stop() {
        threadPool.getDecodeExecutor().shutdown();
        threadPool.getVATScrapperExecutor().shutdownNow();

        return "redirect:/status";
    }


    /**
     * Displays a pageable list of the certificates currently in the database
     *
     * @author Jules Dejaeghere
     * @param model     Model to use to create the HTML page
     * @param page      Current page to show, if empty shows first page
     * @param size      Number of element to show per page, if empty shows 25
     * @param vatonly   If true, displays only the certificates with a VAT number
     * @return          HTML template to use
     */
    @GetMapping("/data")
    public String listCertificates(
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("vatonly") Optional<Boolean> vatonly) {

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(25);
        boolean vatOnly = vatonly.orElse(false);

        List<Certificate> certificatePage
                = certificateService.findPaginatedCertificates((currentPage-1), pageSize, vatOnly);

        model.addAttribute("certificatePage", certificatePage);

        int totalPages = (int) Math.ceil(certificateDao.count() / (float) pageSize);
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        model.addAttribute("nbCert", certificateDao.count());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageNumber", currentPage);
        model.addAttribute("nbPages", totalPages);
        model.addAttribute("vatonly", vatOnly);


        return "data";
    }


    /**
     * Resumes the VAT scrapping of the certificate not yet scrapped in the database
     *
     * @author Jules Dejaeghere
     * @return Redirection to apply
     */
    @GetMapping("/resumevat")
    public String resumeVat() {
        vatScrapingService.resumeVatScrapping();
        return "redirect:/status";
    }


    /**
     * Constructs the HTML page displaying the graph about
     *  - The most popular issuers
     *  - The most popular algorithms
     *  - The status of the VAT scrapping
     *
     * @author Jules Dejaeghere
     * @param model Model to use to create the HTML page
     * @return      HTML template to use
     */
    @GetMapping("/graphs")
    public String graphs(Model model) {

        Pair<ArrayList<BigInteger>, ArrayList<String>> issuerData;
        issuerData = certificateService.issuerGraphData();

        // Set attribute for the issuers graph
        model.addAttribute("dataIssuer", issuerData.getValue0());
        model.addAttribute("labelsIssuer", issuerData.getValue1());


        Pair<ArrayList<BigInteger>, ArrayList<String>> algorithmData;
        algorithmData = certificateService.algorithmGraphData();

        // Set attribute for the algorithm graph
        model.addAttribute("dataAlg", algorithmData.getValue0());
        model.addAttribute("labelsAlg", algorithmData.getValue1());


        ArrayList<Integer> vatCount;
        vatCount = certificateService.vatGraphData();

        // Set the attributes for the VAT numbers
        model.addAttribute("vatCount", vatCount);


        model.addAttribute("count", certificateDao.count());

        return "graphs";
    }


    /**
     * Starts processing of a server and redirects to the server list page
     *
     * @author Jules Dejaeghere
     * @param id    Id of the server to start
     * @return      Redirection to apply
     */
    @GetMapping("/start")
    public String start(@RequestParam("id") long id) {

        Server myServer = serverDao.findById(id);

        if (myServer != null) {
            ScanLogThread scan = new ScanLogThread(myServer, serverService);
            ThreadPool.getServerExecutor().execute(scan);
        }

        return "redirect:/serverList";
    }


    /**
     * Resumes the VAT scrapping for the certificates not yet scrapped in the database
     * Redirects to the home page
     *
     * @author Jules Dejaeghere
     * @return  Redirection to apply
     */
    @GetMapping("/relaunchvat")
    public String relaunchvat() {

        ResumeVATScrapThread thread = new ResumeVATScrapThread(vatScrapingService);
        ThreadPool.getServerExecutor().execute(thread);

        return "redirect:/";
    }


    /**
     * Shuts down the application with an exit code = 0
     *
     * @author Jules Dejaeghere
     * @return  (Nonsense) redirection to apply
     */
    @GetMapping("/shutdown")
    public String shutdown() {
        logger.warn("STOPPING remaining threads, this may take some time.");
        System.exit(0);
        return "redirect:/";
    }

}
