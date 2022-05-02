import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Client {
    FileOutputStream outputStream = null;
    FileInputStream inputStream = null;

    public static void main(String[] args) throws FileNotFoundException {
        new Client().start();
    }

    public void start() throws FileNotFoundException {
        final NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    //максимальный размер сообщения равен 1024*1024 байт, в начале сообщения пдля хранения длины зарезервировано 3 байта,
                                    //которые отбросятся после получения всего сообщения и передачи его дальше по цепочке
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    //Перед отправкой добавляет в начало сообщение 3 байта с длиной сообщения
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new SimpleChannelInboundHandler<Message>() {

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            AuthMessage authMessage1 = new AuthMessage();
                                            authMessage1.setLogin("login1");
                                            authMessage1.setPassword("pass1");
                                            System.out.println("Try to auth: " + authMessage1.getLogin() + "/" + authMessage1.getPassword());
                                            ctx.writeAndFlush(authMessage1);
                                        }

                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws FileNotFoundException {
                                            if (msg instanceof TextMessage){
                                                System.out.println("receive msg " + msg);
                                            }

                                            if (msg instanceof ServerResponseWithContentMessage){
                                                if (outputStream==null) outputStream = new FileOutputStream("/home/tpronicheva/hiiii.zip");
                                                ServerResponseWithContentMessage message = (ServerResponseWithContentMessage) msg;
                                                try{
                                                  outputStream.write(message.getContent());
                                                  if (message.isLast()){
                                                      System.out.println("С сервера получен файл");
                                                      outputStream.close();
                                                      outputStream = null;}
                                                } catch(Exception e){
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                            );
                        }
                    });

            System.out.println("Client started");
            Channel channel = bootstrap.connect("localhost", 9000).sync().channel();


            RequestFileFromServerMessage message = new RequestFileFromServerMessage();
            message.setPath("hi.zip");
            channel.writeAndFlush(message);

            String fileDestinationPoint = "hi.zip";
            String fileSourcePoint = "/home/tpronicheva/video.zip";
            sendFile(channel,fileSourcePoint,fileDestinationPoint);

            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private void sendFile(Channel channel, String fileSourcePoint, String fileDestinationPoint) throws IOException {
        if (inputStream==null) inputStream = new FileInputStream(fileSourcePoint);
        byte[] fileContent = inputStream.readNBytes(64*1024);
        SendFileToServerMessage contentMessage = new SendFileToServerMessage();
        contentMessage.setPath(fileDestinationPoint);
        contentMessage.setLast(inputStream.available()<=0);
        contentMessage.setContent(fileContent);

        channel.writeAndFlush(contentMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (!contentMessage.isLast()){
                    sendFile(channel,fileSourcePoint,fileDestinationPoint);
                }
            }
        });
        if(contentMessage.isLast()){
            System.out.println("На сервер отправлен файл");
            inputStream.close();
            inputStream = null;
        }
    }
}