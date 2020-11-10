import React from 'react';
import { WebView } from 'react-native-webview';

const RoomWebView = () => {
  return (
    <WebView source={{ uri: 'https://reactnative.dev/' }} />
  );
};

export default RoomWebView