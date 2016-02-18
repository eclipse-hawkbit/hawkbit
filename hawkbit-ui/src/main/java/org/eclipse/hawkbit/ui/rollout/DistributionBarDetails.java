package org.eclipse.hawkbit.ui.rollout;

import java.util.HashMap;
import java.util.Map;

public class DistributionBarDetails {
	
	private Map<String,Long> details = new HashMap<>();
	
	
	public DistributionBarDetails( Map<String,Long> details){
		this.details = details;
	}
	
	public Map<String, Long> getDetails() {
		return details;
	}
	
	public void setDetails(Map<String, Long> details) {
		this.details = details;
	}

}
