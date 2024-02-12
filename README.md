# Android Fitbit Middleman
A Android application that received data from Fitbit companion and forward to main server hosted on pc.

## Overview
This application acts as a middle-man connecting Fitbit and PC Main Server. It is built for major purposes below:

1. To received data from Fitbit companion on the *SAME* Android device (i.e. Google Pixel phone).
2. To forward the data received to pc main server.

3. The reason of having this application is due to the restriction that Fitbit companion can only do POST request
to the same device (phone in our case), or a non-self-signed https url. Hence, this Android app helps to achieve the communication
between Fitbit and PC server.

## How to install
To install the application on the devices (phones), connect the phone to the computer that has this project opened in 
Android Studio. On the top right of Android Studio, select the devices connected, and click `run app` button (green triangle).
After running the app, Android studio should install a dev version of the app in the phone. The app is called `System Fitbit Connector` on
the device.

### [Important for devices for teachers] Enable notification from device setting
For the reminder/notification features to work properly, after installation, open the Setting app in the phone, navigate
to Notification, and enable notification for this application (System Fitbit Connector).

## Configuration
Prior installing the application, remember to change the url of the forwarding destination (the main pc server ip). The line to be changed can be found in 
`MainActivity` class in `com.example.systemfitbitconnector`, or do a search for "Replace with actual server URL".

## How to run
After installation, find the app on the phone to open it. The application should start working once opened.

