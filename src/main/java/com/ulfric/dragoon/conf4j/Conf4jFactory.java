package com.ulfric.dragoon.conf4j;

import com.ulfric.conf4j.Configuration;
import com.ulfric.conf4j.interpreter.DataType;
import com.ulfric.conf4j.interpreter.DataTypes;
import com.ulfric.conf4j.interpreter.StandardInterpreterProvider;
import com.ulfric.conf4j.reload.PeriodicalReloadingStrategy;
import com.ulfric.conf4j.reload.ReloadingStrategy;
import com.ulfric.conf4j.source.MultiSource;
import com.ulfric.conf4j.source.Source;
import com.ulfric.conf4j.source.path.DirSource;
import com.ulfric.conf4j.source.path.PathSource;
import com.ulfric.dragoon.Factory;
import com.ulfric.dragoon.ObjectFactory;
import com.ulfric.dragoon.Parameters;
import com.ulfric.dragoon.application.Container;
import com.ulfric.dragoon.extension.inject.Inject;
import com.ulfric.dragoon.qualifier.Qualifier;
import com.ulfric.dragoon.stereotype.Stereotypes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Conf4jFactory implements Factory {

	@Inject
	private ObjectFactory creator;

	@Inject
	private FileSystem fileSystem;

	@Inject(optional = true)
	private Logger logger;

	@Override
	public <T> T request(Class<T> type) {
		return request(type, Parameters.EMPTY);
	}

	@Override
	public <T> T request(Class<T> type, Parameters parameters) { // TODO cache Configuration objects, cleanup
		Path folderOnDisk = folder(parameters.getHolder());
		Qualifier qualifier = parameters.getQualifier();

		Settings settings = Stereotypes.getFirst(qualifier, Settings.class);

		DataType dataType = DataTypes.get(settings.extension());
		String fileName = getFileName(settings, qualifier);

		List<Source> sources = new ArrayList<>();
		Path existingFile = folderOnDisk.resolve(fileName);
		if (Files.isDirectory(existingFile)) {
			sources.add(new DirSource(dataType, existingFile));
		} else {
			sources.add(new PathSource(existingFile));
		}

		Path classPathResource =
				getClassLoaderResource(type.getClassLoader(), defaultFolder().resolve(fileName).toString());
		if (classPathResource != null) {
			sources.add((new PathSource(classPathResource)));
		}
		Source source = new MultiSource(dataType, sources);

		return Configuration.builder()
			.setSource(source)
			.setInterpreter(new StandardInterpreterProvider())
			.setReloadingStrategy(reload(settings))
			.build()
			.as(type);
	}

	private Path getClassLoaderResource(ClassLoader loader, String resource) {
		try {
			resource = resource.replace('\\', '/');
			URI resourceUri = loader.getResource(resource).toURI();
			getJarFileSystem(resourceUri);
			return Paths.get(resourceUri); // TODO use fileSystem field?
		} catch (Exception exception) {
			if (logger != null) {
				logger.log(Level.SEVERE, "Exception finding resource " + resource + " in loader " + loader, exception);
			}
			return null;
		}
	}

	private FileSystem getJarFileSystem(URI resource) {
		try {
			return FileSystems.getFileSystem(resource);
		} catch (FileSystemNotFoundException expected) {
			Map<String, String> environment = Collections.singletonMap("create", "true");
			try {
				return FileSystems.newFileSystem(resource, environment);
			} catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
		}
	}

	private ReloadingStrategy reload(Settings settings) {
		Reload reload = settings.reload();

		if (reload.never()) {
			return null;
		}

		return creator.request(PeriodicalReloadingStrategy.class, reload.period(), reload.unit());
	}

	private String getFileName(Settings settings, Qualifier qualifier) {
		String name = name(settings, qualifier);
		if (settings.appendExtension()) {
			return name + '.' + extension(settings);
		}
		return name;
	}

	private String name(Settings settings, Qualifier qualifier) {
		String name = settings.value();
		if (name.isEmpty()) {
			return qualifier.getName();
		}
		return name;
	}

	private String extension(Settings settings) {
		String extension = settings.extension();
		if (extension.isEmpty()) {
			return SettingsExtension.DEFAULT_FILE_EXTENSION;
		}
		return extension;
	}

	private Path folder(Object owning) {
		Path root = defaultFolder();
		if (owning == null) {
			return root;
		}

		Container container = Container.getOwningContainer(owning);
		if (container == null) {
			return root;
		}

		String name = container.getName();
		if (name == null) {
			return root;
		}

		return root.resolve(name.toLowerCase()); // TODO add hyphens
	}

	private Path defaultFolder() {
		return fileSystem.getPath("settings");
	}

}
