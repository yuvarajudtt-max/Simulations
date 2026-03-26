package com.dtt.simulations.repo;


import com.dtt.simulations.model.HospitalInsurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalInsuranceRepo extends JpaRepository<HospitalInsurance, Integer> {





    @Query("SELECT h FROM HospitalInsurance h ORDER BY h.updatedOn DESC")
    List<HospitalInsurance> getHospitalInsuranceDetails();


    @Query("SELECT h FROM HospitalInsurance h WHERE h.id = ?1")
    HospitalInsurance getHospitalInsuranceDetailsById(int id);
}
