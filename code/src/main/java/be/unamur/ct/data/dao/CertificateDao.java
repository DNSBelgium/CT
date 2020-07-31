package be.unamur.ct.data.dao;

import be.unamur.ct.decode.model.Certificate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateDao extends JpaRepository<Certificate, Integer> {
    List<Certificate> findAllByOrderByIdAsc(Pageable pageable);

    List<Certificate> findAllByVATNotNull();

    List<Certificate> findAllByVATNotNullOrderByIdAsc(Pageable pageable);

    List<Certificate> findByVatSearched(boolean value);

    @Query(value = "select issuer, count(*) as num from certificate group by issuer", nativeQuery = true)
    List<Object[]> distinctIssuer();

    @Query(value = "select signature_alg, count(*) as num from certificate group by signature_alg", nativeQuery = true)
    List<Object[]> distinctAlgorithm();

    Integer countByVATIsNotNullAndVatSearched(boolean vatSearched);

    Integer countByVATIsNullAndVatSearched(boolean vatSearched);

    long count();
}
