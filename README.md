<p align="center">
  <img src="src/main/resources/com/uniqueapps/musemix/logo.png" alt="Maxis Logo" width="500"/>
</p>

# Musemix

A modern JavaFX application for accessing and playing any MIDI instrument available on your device, and a full-featured high performance composer to load, create, edit and save MIDI compositions.

### Secondary Goal

This application demonstrates how to build JavaFX applications with a custom modern theme that can be compiled into a native executable using GraalVM and Gluon.

## Features

- **Access All MIDI Instruments**: Browse and play any of the standard MIDI instruments available on your system
- **MIDI Composer**: Create, edit, and save MIDI compositions with an intuitive interface
- **MIDI Playback**: Play MIDI files with accuracy and performance, even with large Black MIDIs
- **Interactive Controls**: Experiment with any MIDI instrument on the go
- **Modern UI**: Built with AtlantaFX's PrimerDark theme for a sleek, modern appearance
- **Native Compilation**: Can be compiled to native executables using GraalVM and Gluon for improved startup time and reduced memory footprint

## Prerequisites

- **Java 21** or higher (corresponding to GraalVM version preferred)
- **JavaFX 21** or higher (limited by Gluon supplied GraalVM+JavaFX versions)
- **Maven 3**
- **GraalVM** (optional, for native compilation)

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/UnknownCoder56/musemix.git
cd musemix
```

### Build and Run

#### Running with Maven

(Comment out "java.home" override first, in MusemixApplication.java, if you want to run on standard JDK instead of native image)

```bash
mvn clean javafx:run
```

This will compile the application and launch it using the JavaFX Maven plugin.

## Native Compilation with GraalVM

This application can be compiled into a native executable using GraalVM and Gluon's GluonFX plugin.

### Prerequisites for Native Compilation

1. **Install GraalVM**: Download and install a Gluon-supplied GraalVM
2. **Set GRAALVM_HOME**: Update the `graalvmHome` path in `pom.xml` to point to your GraalVM installation:

```xml
<graalvmHome>/path/to/your/graalvm-installation</graalvmHome>
```
### Run Tracer Agent

```bash
mvn clean gluonfx:runagent
```

Then go through the application to cover all code paths that you want to include in the native image. This step generates the necessary configuration files for native compilation.

### Build Native Image

```bash
mvn clean gluonfx:build
```

### Run Native Image

Warning! Make sure to download jsound.dll and place it in the same directory as the generated native executable, otherwise MIDI device access will fail.

```bash
mvn gluonfx:nativerun
```

Or simply run the generated executable file. The native executable will provide faster startup times and lower memory consumption compared to running on the JVM.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

Arpan Chatterjee

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
