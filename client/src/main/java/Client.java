import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;


public class Client {

    public static void main(String[] args) throws FileNotFoundException {
        new Client().start();
    }

    public void start() throws FileNotFoundException {
        FileOutputStream outputStream = new FileOutputStream("/home/tpronicheva/video2.zip");
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
                                        int counter = 0;

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            AuthMessage authMessage1 = new AuthMessage();
                                            authMessage1.setLogin("login1");
                                            authMessage1.setPassword("pass1");
                                            System.out.println("Try to auth: " + authMessage1.getLogin() + "/" + authMessage1.getPassword());
                                            ctx.writeAndFlush(authMessage1);
                                        }

                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
                                            if (msg instanceof TextMessage){
                                                System.out.println("receive msg " + msg);
                                            }
                                            if (msg instanceof FileContentMessage){
                                                FileContentMessage message = (FileContentMessage) msg;
                                                try{
                                                  outputStream.write(message.getContent());
                                                  counter ++;
                                                  System.out.println(counter + " packages received");
                                                  if (message.isLast()){ctx.close();}
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

            FileRequestMessage message = new FileRequestMessage();
            message.setPath("/home/tpronicheva/video.zip");
            channel.writeAndFlush(message);

            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}