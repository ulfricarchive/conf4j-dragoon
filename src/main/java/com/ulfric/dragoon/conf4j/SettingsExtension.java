package com.ulfric.dragoon.conf4j;

import com.ulfric.dragoon.Factory;
import com.ulfric.dragoon.extension.Extension;
import com.ulfric.dragoon.extension.inject.Inject;
import com.ulfric.dragoon.reflect.Classes;
import com.ulfric.dragoon.reflect.FieldProfile;
import com.ulfric.dragoon.reflect.LazyFieldProfile;

public class SettingsExtension extends Extension {

	public static final String DEFAULT_FILE_EXTENSION = "yml";

	private final LazyFieldProfile fields = new LazyFieldProfile(this::createFieldProfile);

	@Inject
	private Factory parent;

	private boolean loading;

	private FieldProfile createFieldProfile() {
		loading = true;
		FieldProfile field = FieldProfile.builder()
				.setFactory(parent.request(Conf4jFactory.class))
				.setFlagToSearchFor(Settings.class)
				.build();
		loading = false;
		return field;
	}

	@Override
	public <T> T transform(T value) {
		if (!loading && value instanceof Configured) {
			fields.accept(value);
		}
		return value;
	}

	@Override
	public <T> Class<? extends T> transform(Class<T> type) {
		if (!loading && fields.test(type)) {
			return Classes.implement(type, Configured.class);
		}
		return type;
	}

}
