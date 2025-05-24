package com.chtrembl.petstoreapp.model;

import lombok.Data;

import java.io.Serializable;

@Data

public class Version implements Serializable {
	private String version;
	private String date;
}
