package com.dtt.simulations.POA.repo;


import com.dtt.simulations.POA.model.PowerOfAttorney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PoaPowerOfAttorneyRepo extends JpaRepository<PowerOfAttorney, Integer> {


    @Query("SELECT p FROM PowerOfAttorney p " +
            "WHERE p.principleIdDocNumber = :idDocNumber " +
            "AND p.status = 'APPROVED'")
    PowerOfAttorney fetchApprovedPoaRequests(@Param("idDocNumber") String idDocNumber);


}
