import ceylon.io.buffer {
    ByteBuffer
}
import ceylon.net.http.server.websocket {
    WebSocketChannel,
    CloseReason
}

import io.undertow.util {
    Headers {
        hostStringHeader=HOST_STRING
    }
}
import io.undertow.websockets.core {
    UtWebSocketChannel=WebSocketChannel,
    WebSockets {
        wsSendBinary=sendBinary,
        wsSendBinaryBlocking=sendBinaryBlocking,
        wsSendText=sendText,
        wsSendTextBlocking=sendTextBlocking,
        wsSendClose=sendClose,
        wsSendCloseBlocking=sendCloseBlocking
    },
    CloseMessage,
    WebSocketException
}
import io.undertow.websockets.spi {
    WebSocketHttpExchange
}

import java.nio { JByteBuffer = ByteBuffer }

by("Matej Lazar")
class DefaultWebSocketChannel(WebSocketHttpExchange exchange, 
    UtWebSocketChannel channel) 
        satisfies WebSocketChannel {

    shared UtWebSocketChannel underlyingChannel => channel;

    closeFrameReceived => channel.closeFrameReceived;

    idleTimeout => channel.idleTimeout;

    open() => channel.open;

    schema => channel.requestScheme;

    fragmentedTextSender() 
            => DefaultFragmentedTextSender(this);

    fragmentedBinarySender() 
            => DefaultFragmentedBinarySender(this);

    JByteBuffer getUnderlayingImplementation(ByteBuffer binary) {
        Object? javaByteBuffer = binary.implementation;
        if (is JByteBuffer javaByteBuffer) {
            return javaByteBuffer;
        } else {
            throw WebSocketException("Underlaying implementation of ByteBuffer must be of type java.nio.ByteBuffer.");
        }
    }

    sendBinary(ByteBuffer binary) => wsSendBinaryBlocking(getUnderlayingImplementation(binary), channel);

    sendBinaryAsynchronous(
            ByteBuffer binary,
            Anything(WebSocketChannel) onCompletion,
            Anything(WebSocketChannel,Throwable)? onError)
            => wsSendBinary(getUnderlayingImplementation(binary), channel, 
                wrapCallbackSend(onCompletion, onError, this));

    sendText(String text) => wsSendTextBlocking(text, channel);

    sendTextAsynchronous(
            String text,
            Anything(WebSocketChannel) onCompletion,
            Anything(WebSocketChannel,Throwable)? onError) 
            => wsSendText(text, channel, wrapCallbackSend(onCompletion, onError, this));

    sendClose(CloseReason reason) 
            => wsSendCloseBlocking(CloseMessage(reason.code, reason.reason else "").toByteBuffer(), channel);

    hostname => exchange.getRequestHeader(hostStringHeader);

    requestPath => exchange.requestURI;

    sendCloseAsynchronous(
            CloseReason reason,
            Anything(WebSocketChannel) onCompletion,
            Anything(WebSocketChannel,Throwable)? onError) 
            => wsSendClose(
                CloseMessage(reason.code, reason.reason else "").toByteBuffer(),
                channel,
                wrapCallbackSend(onCompletion, onError, this));
}
