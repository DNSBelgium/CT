package be.unamur.ct;


import be.unamur.ct.download.model.Server;
import be.unamur.ct.download.service.ServerService;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;


/*
 * The database specified in the application.properties file should be running in order to run these test.
 * These tests will initialize the complete ApplicationContext to run, including the database
 * No changes will be made to the database, the application will only try to connect
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class ServerServiceTest {

    @Autowired
    private ServerService serverService;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(8080));

    private Server server;

    @Before
    public void setup() throws IOException {

        server = new Server();
        server.setUrl("http://localhost:8080/");
        server.setNickname("Local test server");
        server.setId(1);

        //  Creating small test web server to serve web pages to scrap
        byte[] body = FileCopyUtils.copyToByteArray( new ClassPathResource("json/sth.json").getInputStream());

        wireMockRule.stubFor(get(urlEqualTo("/ct/v1/get-sth"))
                .willReturn(aResponse()
                        .withBody(body)
                        .withHeader("Content-Type", "application/json")
                )
        );
    }


    @Test
    public void testCheckSize(){
        long size;
        size = serverService.checkSize(server);
        assertThat(size).isEqualTo(3);
    }


}

