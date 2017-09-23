# conf4j-dragoon
Dragoon CDI support for Conf4j

```java
public static void main(String[] args) {
	ObjectFactory factory = new ObjectFactory();
	factory.install(SettingsExtension.class, factory);

	Greeting greeting = factory.request(Greeting.class);
	greeting.sayHello();
}

public static class Greeting {

	@Settings
	private Hello config;

	public void sayHello() {
		System.out.println(config.message());
	}

}

public interface Hello extends ConfigurationBean {
	String message();
}
```