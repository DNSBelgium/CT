package be.unamur.ct;


import be.unamur.ct.data.dao.ServerDao;
import be.unamur.ct.data.dao.SliceDao;
import be.unamur.ct.download.model.Server;
import be.unamur.ct.download.model.Slice;
import com.google.common.collect.Ordering;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@DataJpaTest
public class SliceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SliceDao sliceDao;

    @Autowired
    private ServerDao serverDao;

    @SuppressWarnings("FieldCanBeLocal")
    private Slice slice1, slice2, slice3;
    private Server server;

    @Before
    public void setupDatabase(){

        server = new Server("http://test.com/", "Test Server");

        server = entityManager.persist(server);

        slice1 = new Slice(0, 99, 0, server);
        slice2 = new Slice(100, 199, 100, server);
        slice3 = new Slice(200, 299, 200, server);

        slice1 = entityManager.persist(slice1);
        slice2 = entityManager.persist(slice2);
        slice3 = entityManager.persist(slice3);

        entityManager.flush();
    }


    @Test
    public void testExistsAndDeleteById(){
        boolean s1 = sliceDao.existsById(slice1.getId());
        boolean none = sliceDao.existsById(199);

        assertTrue(s1);
        assertFalse(none);

        sliceDao.deleteById(slice1.getId());
        s1 = sliceDao.existsById(slice1.getId());

        assertFalse(s1);
    }


    @Test
    public void testFindByServerOrderByEndSliceDesc(){
        Server s = serverDao.findById(server.getId());
        List<Slice> slices = sliceDao.findByServerOrderByEndSliceDesc(s);

        List<Long> endSlice = new ArrayList<>();
        for (Slice slice : slices){
            endSlice.add(slice.getEndSlice());
        }
        boolean sorted = Ordering.natural().reverse().isOrdered(endSlice);
        assertTrue(sorted);
    }

}
