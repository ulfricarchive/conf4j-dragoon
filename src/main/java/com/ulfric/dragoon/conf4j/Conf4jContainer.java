package com.ulfric.dragoon.conf4j;

import com.ulfric.dragoon.application.Container;

public class Conf4jContainer extends Container {

	public Conf4jContainer() {
		install(SettingsExtension.class);
	}

}