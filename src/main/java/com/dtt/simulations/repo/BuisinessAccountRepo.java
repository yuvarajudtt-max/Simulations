package com.dtt.simulations.repo;


import com.dtt.simulations.model.BusinessAccountModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuisinessAccountRepo extends JpaRepository<BusinessAccountModel,Integer> {




    @Query("SELECT b FROM BusinessAccountModel b WHERE b.id = ?1 ORDER BY b.createdOn DESC")
    BusinessAccountModel getDetailsById(int id);


    @Query("SELECT b FROM BusinessAccountModel b WHERE b.passportNumber = ?1 ORDER BY b.id DESC")
    BusinessAccountModel getByPassportId(String passportNumber);


    @Query("SELECT b FROM BusinessAccountModel b ORDER BY b.id DESC")
    List<BusinessAccountModel> getAllData();
}
