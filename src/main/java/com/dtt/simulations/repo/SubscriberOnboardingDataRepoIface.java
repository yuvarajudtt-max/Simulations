/**
 *
 */
package com.dtt.simulations.repo;

import com.dtt.simulations.model.SubscriberOnboardingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberOnboardingDataRepoIface extends JpaRepository<SubscriberOnboardingData, Integer> {

    SubscriberOnboardingData findBysubscriberUid(String suid);


    @Query("SELECT s FROM SubscriberOnboardingData s WHERE s.subscriberUid = :uid ORDER BY s.createdDate DESC")
    List<SubscriberOnboardingData> getBySubUid(@Param("uid") String uid);


    @Query("SELECT s FROM SubscriberOnboardingData s WHERE s.subscriberUid = :suid ORDER BY s.createdDate DESC")
    SubscriberOnboardingData findLatestSubscriber(@Param("suid") String suid);

    @Query("SELECT COUNT(s) FROM SubscriberOnboardingData s WHERE s.optionalData1 = :optionalData1")
    int getOptionalData1(@Param("optionalData1") String optionalData1);


    @Query("SELECT DISTINCT s.subscriberUid FROM SubscriberOnboardingData s WHERE s.optionalData1 = :optionalData1")
    String getOptionalData1Subscriber(@Param("optionalData1") String optionalData1);


    @Query("SELECT s FROM SubscriberOnboardingData s WHERE s.idDocNumber = :documentNumber")
    List<SubscriberOnboardingData> findSubscriberByDocId(@Param("documentNumber") String documentNumber);


    @Query("SELECT s FROM SubscriberOnboardingData s")
    List<SubscriberOnboardingData> getAllSelfies();

    Optional<SubscriberOnboardingData>
    findTopByIdDocNumberIgnoreCaseOrderByCreatedDateDesc(String idDocNumber);




}
