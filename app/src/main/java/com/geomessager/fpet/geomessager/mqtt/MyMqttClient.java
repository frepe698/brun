package com.geomessager.fpet.geomessager.mqtt;

import android.app.Activity;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.io.UnsupportedEncodingException;


public class MyMqttClient implements MqttCallback{
    private String clientId;
    private MqttAndroidClient client;

    public MyMqttClient(Activity activity){
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(activity.getApplicationContext(), "tcp://broker.hivemq.com:1883", clientId);
        client.setCallback(this);
    }

    public void Connect(){
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("mqtt", "Successful connection established");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("mqtt", "Failed to connect to server" + exception.toString());
                }
            });
        } catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void Disconnect(){
        try{
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("mqtt", "Disconnected from server");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("mqtt", "Failed to disconnect from server");
                }
            });
        } catch(MqttException e){
            e.printStackTrace();
        }
    }

    public void Publish(String topic, String payload){
        byte[] encodedPayload;
        try{
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            message.setRetained(true);
            client.publish(topic, message);
            Log.d("mqtt", "Published new message: " + message + " to topic " + topic);
        } catch(UnsupportedEncodingException | MqttException e){
            e.printStackTrace();
        }
    }

    public void Subscribe(final String topic){
        try {
            IMqttToken token = client.subscribe(topic, 1);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("mqtt", "Succesfully subscribed to topic: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("mqtt", "Failed to subscribe to topic: " + topic);
                }
            });
        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    public boolean isConnected(){
        return client.isConnected();
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d("mqtt", "Lost connection");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.d("mqtt", "Received message from topic: " + topic + "with message: " + message.toString());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d("mqtt", "Delivery complete");
    }
}
