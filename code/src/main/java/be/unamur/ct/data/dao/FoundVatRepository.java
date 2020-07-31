package be.unamur.ct.data.dao;

import be.unamur.ct.scrap.model.FoundVat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoundVatRepository  extends JpaRepository<FoundVat, Integer> {

}
