/**
 * 
 */
package com.dtt.simulations.repo;

import com.dtt.simulations.model.SubscriberStatusModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;




@Repository
public interface SubscriberStatusRepoIface extends JpaRepository<SubscriberStatusModel, Integer>{

	SubscriberStatusModel findBysubscriberUid(String suid);
	
}
