package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import java.io.Serializable;

public class CustomObject implements Serializable {
	private static final long serialVersionUID = -5018181529953620263L;

	private String name;

	private String status;

	public CustomObject(){
		
	}
	
	public CustomObject(String name, String status) {
		super();
		this.name = name;
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}

}
