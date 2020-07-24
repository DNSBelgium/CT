package be.unamur.ct;


import be.unamur.ct.data.dao.ServerDao;
import be.unamur.ct.download.model.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ServerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ServerDao serverDao;

    @SuppressWarnings("FieldCanBeLocal")
    private Server srv1, srv2;

    @Before
    public void setupDatabase(){

        srv1 = new Server("http://www.test-server.com/", "First Test");
        srv2 = new Server("http://www.test.com/", "Second Test");
        srv1 = entityManager.persist(srv1);
        srv2 = entityManager.persist(srv2);

        entityManager.flush();
    }


    @Test
    public void testFindById(){
        Server s1 = serverDao.findById(srv1.getId());
        Server s2 = serverDao.findById(-10);


        assertNotNull(s1);
        assertNull(s2);
    }


    @Test
    public void testExistsByUrl(){
        boolean exists1 = serverDao.existsByUrl("http://www.test.com/");
        boolean exists2 = serverDao.existsByUrl("http://www.not-found.com/");


        assertTrue(exists1);
        assertFalse(exists2);
    }
}
