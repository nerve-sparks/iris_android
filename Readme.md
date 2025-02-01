<h2><a href="https://play.google.com/store/apps/details?id=com.nervesparks.irisGPT&hl=en_IN"  style="color: white;">Iris</a></h2>

## Project Description

- This repository contains llama.cpp based offline android chat application cloned from llama.cpp android example. Install, download model and run completely offline privately.
- The app supports downloading GGUF models from Hugging Face and offers customizable parameters for flexible use.
- Being open-source, it allows for easy modifications and improvements, providing a secure, private, and fully offline experience.

## Images

<div style="display: flex; gap: 15px; justify-content: center; flex-wrap: wrap;">
  <div style="text-align: center; width: 200px;">
    <img src="./images/main_screen.png" alt="Main Screen Screenshot" width="200">
    <p><strong>Main Screen</strong></p>
    <p>This is the main interface of the app where users can access all core functionalities.</p>
  </div>
  <div style="text-align: center; width: 200px;">
    <img src="./images/chat_screen.png" alt="Chat Screen Screenshot" width="200">
    <p><strong>Chat Screen</strong></p>
    <p>The chat feature allows users to interact in real-time and access AI-driven responses.</p>
  </div>
  <div style="text-align: center; width: 200px;">
    <img src="./images/settings_screen.png" alt="Settings Screen Screenshot" width="200">
    <p><strong>Settings Screen</strong></p>
    <p>Users can customize app preferences and configure their account settings here.</p>
  </div>
  <div style="text-align: center; width: 200px;">
    <img src="./images/models_screen.png" alt="Models Screen Screenshot" width="200">
    <p><strong>Models Screen</strong></p>
    <p>This screen displays available AI models and allows users to manage them efficiently.</p>
  </div>
  <div style="text-align: center; width: 200px;">
    <img src="./images/parameters_screen.png" alt="Parameters Screen Screenshot" width="200">
    <p><strong>Parameters Screen</strong></p>
    <p>Users can adjust parameters to fine-tune the app's performance based on their needs.</p>
  </div>
</div>

## Installation
- Get IRIS (offline GPT) on Google Play:

  [Get it on Google Play](https://play.google.com/store/apps/details?id=com.nervesparks.irisGPT&hl=en_IN)
## Run

- Go to releases : https://github.com/nerve-sparks/iris_android/releases
- Download app
- Install app

## Features

- Works Offline: Access all features without needing an internet connection.
- Privacy-Focused: All data is processed securely on your device.
- Expandable Models: Download external GGUF models from Hugging Face.
- Open Source: Fully transparent development.
- Customizable Parameters: n_threads, top_k, top_p, and temperature can be adjusted to optimize performance and behavior based on device capabilities and desired output.
- Text To Speech: Support for Text-to-Speech functionality.
- Speech To Text: Support for Speech-to-Text functionality.
- Default Model Selection: Set a default model to load automatically on startup.

## Optimizing Your Experience with Iris

The performance of Iris is directly influenced by the size, speed, and compute requirements of the models you use. These factors also impact the overall user experience. For a faster and smoother experience, consider downloading smaller models.

**Example Recommendation:**  
On opening the app, users can download suggested models to optimize performance based on their preferences and device capabilities.

---

#### Note:

- Smaller models are ideal for quicker interactions but may compromise slightly on response quality.
- Larger models offer more comprehensive responses but require higher compute power and may result in slower speeds.
- Choose a model that best balances speed and quality for your specific use case.

---

#### Disclaimer:

- Iris may produce **inaccurate results** depending on the complexity of queries and model limitations.
- Performance and accuracy are influenced by the size and type of model selected.

## Build

- Download Android studio
- Clone this repository and import into Android Studio

```bash
 git clone https://github.com/nerve-sparks/iris_android.git
```

- Clone the llama.cpp repository in the same folder as iris_android

```bash
 git clone https://github.com/ggerganov/llama.cpp
```
- Navigate to the llama.cpp directory and checkout a specific commit for proper compatibility:

```bash
cd llama.cpp
git checkout 1f922254f0c984a8fb9fbaa0c390d7ffae49aedb
cd ..
```
- Open developer options on the Android mobile phone.
- Enable developer options.
- Click on developer options and enable wireless debugging.
- In Android Studio, select the drop down on the left side of the 'app' button on the Navbar.
- Select on 'Pair devices using Wi-Fi'. A QR code appears on screen.
- Click on wireless debugging on Android phone. Select 'Pair device with QR code'. Scan the code. (Make sure both devices are on the same Wi-fi)
- You can use Usb Debugging also to connect your phone.
- Once the phone is connected, select the device name in the drop down menu and click on play button.
- In the app, download at least one model from the given options.
- Now you can run the app offline. (In airplane mode as well)

## Contributing

1. **Fork the repository.**
2. **Create a new feature branch:**
   ```bash
   git checkout -b my-new-feature
   ```
3. **Commit your changes:**
   ```bash
   git commit -m 'Add some feature'
   ```
4. **Push your branch to the remote repository:**
   ```bash
   git push origin my-new-feature
   ```
5. **Open a Pull Request.**

## Maintained by Nerve Sparks

- Visit www.nervesparks.com to contact us.
