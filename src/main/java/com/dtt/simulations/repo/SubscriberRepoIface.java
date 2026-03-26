package com.dtt.simulations.repo;


import com.dtt.simulations.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;




@Repository
@Transactional
public interface SubscriberRepoIface extends JpaRepository<Subscriber, Integer>{




	@Query("SELECT s.videoUrl FROM SubscriberCompleteDetails s WHERE s.subscriberUid = :subscriberUid")
	String getSubscriberUid(@Param("subscriberUid") String subscriberUid);

	@Query("""
       SELECT s
       FROM Subscriber s
       WHERE LOWER(s.idDocNumber) = LOWER(:idDocument)
       """)
	Subscriber findbyDocumentNumber(@Param("idDocument") String idDocument);

	// Keep method name: getSuid
	@Query("SELECT s.subscriberUid FROM Subscriber s WHERE s.idDocNumber = :passportno")
	String getSuid(@Param("passportno") String passportno);





	@Query("""
       SELECT s
       FROM Subscriber s
       WHERE LOWER(s.idDocNumber) = LOWER(:passportno)
       """)
	Subscriber getSubscriberDetail(@Param("passportno") String passportno);


	Subscriber findByPassportNumber(String passportNumber);
	Subscriber findByNationalIdNumber(String nationalIdNumber);
	Subscriber findBySubscriberUid(String suid);

}
