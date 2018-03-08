package nia.chapter5;

import io.netty.buffer.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ByteProcessor;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;

import static io.netty.channel.DummyChannelHandlerContext.DUMMY_INSTANCE;
import static org.junit.Assert.*;

/**
 * Created by ryanjiang on 2018/3/6.
 */
public class ByteBufExamplesTest {

    private final static Random random = new Random();
    private static final ByteBuf BYTE_BUF_FROM_SOMEWHERE = Unpooled.buffer(1024);
    private static final Channel CHANNEL_FROM_SOMEWHERE = new NioSocketChannel();
     private static final ChannelHandlerContext CHANNEL_HANDLER_CONTEXT_FROM_SOMEWHERE = DUMMY_INSTANCE;
    // private static final ChannelHandlerContext CHANNEL_HANDLER_CONTEXT_FROM_SOMEWHERE = null;

    @Test
    public void primitiveIo() throws Exception {
        ByteBuf bf = Unpooled.copyDouble(1.2, 3.3);
        assertEquals(bf.readerIndex(), 0);
        assertEquals(bf.writerIndex(), 2 * 8);  // 写了两个double

        assertEquals(bf.readDouble(), 1.2, 0.01);
        assertEquals(bf.readDouble(), 3.3, 0.01);
        assertEquals(bf.readerIndex(), 2 * 8); // 读了两个double

        // bf = Unpooled.copyInt(1, 2, 3);
        // bf = Unpooled.copyBoolean(true, false);
    }

    @Test
    public void stringIo() throws Exception {
        ByteBuf bf = Unpooled.copiedBuffer("hello netty!", Charset.forName("UTF-8"));
        assertEquals(bf.readerIndex(), 0);
        assertEquals(bf.writerIndex(), 1 * 12);  // 写了12 byte

        CharSequence cs = bf.readCharSequence(bf.readableBytes(), Charset.forName("UTF-8"));
        assertEquals(cs, "hello netty!");
    }

    @Test
    public void readForward() throws Exception {
        ByteBuf bf = Unpooled.copiedBuffer("hello netty!", Charset.forName("UTF-8"));

        while (bf.isReadable()) {
            System.out.print((char) bf.readByte());
        }
    }

    @Test
    public void writeForward() throws Exception {
        ByteBuf bf = Unpooled.buffer(32); // 32 bytes

        int count = 0;
        while (bf.writableBytes() >= 4) {
            System.out.println("writable: " + bf.writableBytes() + ", count: " + count);
            bf.writeInt(count);
            count++;
        }
        while (bf.isReadable()) {
            System.out.println(bf.readInt());
        }
    }

    @Test
    public void randomAccess() throws Exception {
        ByteBuf bf = Unpooled.copiedBuffer("hello netty!", Charset.forName("UTF-8"));

        for (int i = 0; i < bf.readableBytes(); i++) {
            System.out.print((char) bf.getByte(i));
        }
    }

    @Test
    public void discardRead() throws Exception {
        ByteBuf bf = Unpooled.copiedBuffer("hello netty!", Charset.forName("UTF-8"));
        CharSequence cs = bf.readCharSequence(bf.readableBytes(), Charset.forName("UTF-8"));
        assertEquals(cs, "hello netty!");

        bf.discardReadBytes();  // 清空了读写索引
        assertEquals(bf.readerIndex(), 0);
        assertEquals(bf.writerIndex(), 0);
    }

    @Test
    public void searchSomething() throws Exception {
        ByteBuf bf = Unpooled.copiedBuffer("hello netty!", Charset.forName("UTF-8"));
        int index = bf.forEachByte(new ByteProcessor.IndexOfProcessor((byte)'t'));
        assertEquals(index, "hello netty!".indexOf('t'));
    }

    /**
     * Listing 5.1 Backing array
     */
    @Test
    public void heapBuffer() {
        ByteBuf heapBuf = Unpooled.buffer(1024);
        if (heapBuf.hasArray()) {
            byte[] array = heapBuf.array();
            int offset = heapBuf.arrayOffset() + heapBuf.readerIndex();
            int length = heapBuf.readableBytes();
            handleArray(array, offset, length);
        }
    }

    /**
     * Listing 5.2 Direct buffer data access
     */
    public void directBuffer() {
        ByteBuf directBuf = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        if (!directBuf.hasArray()) {
            int length = directBuf.readableBytes();
            byte[] array = new byte[length];
            directBuf.getBytes(directBuf.readerIndex(), array);
            handleArray(array, 0, length);
        }
    }

    /**
     * Listing 5.3 Composite buffer pattern using ByteBuffer
     */
    public void byteBufferComposite(ByteBuffer header, ByteBuffer body) {
        // Use an array to hold the message parts
        ByteBuffer[] message =  new ByteBuffer[]{ header, body };

        // Create a new ByteBuffer and use copy to merge the header and body
        ByteBuffer message2 =
                ByteBuffer.allocate(header.remaining() + body.remaining());
        message2.put(header);
        message2.put(body);
        message2.flip();
    }


    /**
     * Listing 5.4 Composite buffer pattern using CompositeByteBuf
     */
    public void byteBufComposite() {
        CompositeByteBuf messageBuf = Unpooled.compositeBuffer();
        ByteBuf headerBuf = BYTE_BUF_FROM_SOMEWHERE; // can be backing or direct
        ByteBuf bodyBuf = BYTE_BUF_FROM_SOMEWHERE;   // can be backing or direct
        messageBuf.addComponents(headerBuf, bodyBuf);
        //...
        messageBuf.removeComponent(0); // remove the header
        for (ByteBuf buf : messageBuf) {
            System.out.println(buf.toString());
        }
    }

    /**
     * Listing 5.5 Accessing the data in a CompositeByteBuf
     */
    public void byteBufCompositeArray() {
        CompositeByteBuf compBuf = Unpooled.compositeBuffer();
        int length = compBuf.readableBytes();
        byte[] array = new byte[length];
        compBuf.getBytes(compBuf.readerIndex(), array);
        handleArray(array, 0, array.length);
    }

    /**
     * Listing 5.6 Access data
     */
    public void byteBufRelativeAccess() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        for (int i = 0; i < buffer.capacity(); i++) {
            byte b = buffer.getByte(i);
            System.out.println((char) b);
        }
    }

    /**
     * Listing 5.7 Read all data
     */
    public void readAllData() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        while (buffer.isReadable()) {
            System.out.println(buffer.readByte());
        }
    }

    /**
     * Listing 5.8 Write data
     */
    public void write() {
        // Fills the writable bytes of a buffer with random integers.
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        while (buffer.writableBytes() >= 4) {
            buffer.writeInt(random.nextInt());
        }
    }

    /**
     * Listing 5.9 Using ByteProcessor to find \r
     *
     * use {@link io.netty.buffer.ByteBufProcessor in Netty 4.0.x}
     */
    public void byteProcessor() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        int index = buffer.forEachByte(ByteProcessor.FIND_CR);
    }

    /**
     * Listing 5.9 Using ByteBufProcessor to find \r
     *
     * use {@link io.netty.util.ByteProcessor in Netty 4.1.x}
     */
    public void byteBufProcessor() {
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        int index = buffer.forEachByte(ByteBufProcessor.FIND_CR);
    }

    /**
     * Listing 5.10 Slice a ByteBuf
     */
    @Test
    public void byteBufSlice() {
        Charset utf8 = Charset.forName("UTF-8");
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        ByteBuf sliced = buf.slice(0, 15);
        System.out.println(sliced.toString(utf8));
        buf.setByte(0, (byte)'J');
        assert buf.getByte(0) == sliced.getByte(0);
        System.out.println(sliced.toString(utf8));
        System.out.println(buf.toString(utf8));
    }

    /**
     * Listing 5.11 Copying a ByteBuf
     */
    @Test
    public void byteBufCopy() {
        Charset utf8 = Charset.forName("UTF-8");
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        ByteBuf copy = buf.copy(0, 15);
        System.out.println(copy.toString(utf8));
        copy.setByte(0, (byte)'J');
        assert buf.getByte(0) != copy.getByte(0);
        System.out.println(copy.toString(utf8));
        System.out.println(buf.toString(utf8));
    }

    /**
     * Listing 5.12 get() and set() usage
     */
    @Test
    public void byteBufSetGet() {
        Charset utf8 = Charset.forName("UTF-8");
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        System.out.println((char)buf.getByte(0));
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        buf.setByte(0, (byte)'B');
        System.out.println((char)buf.getByte(0));
        assert readerIndex == buf.readerIndex();
        assert writerIndex == buf.writerIndex();
    }

    /**
     * Listing 5.13 read() and write() operations on the ByteBuf
     */
    @Test
    public void byteBufWriteRead() {
        Charset utf8 = Charset.forName("UTF-8");
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        System.out.println((char)buf.readByte());
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        buf.writeByte((byte)'?');
        assert readerIndex == buf.readerIndex();
        assert writerIndex != buf.writerIndex();
    }

    private static void handleArray(byte[] array, int offset, int len) {}

    /**
     * Listing 5.14 Obtaining a ByteBufAllocator reference
     */
    @Test
    public void obtainingByteBufAllocatorReference(){
        Channel channel = CHANNEL_FROM_SOMEWHERE; //get reference form somewhere
        ByteBufAllocator allocator = channel.alloc();
        ByteBuf buf = allocator.buffer(32);

        int count  = 0;
        while (buf.writableBytes() >= 4) {
            buf.writeInt(count);
            count++;
        }

        while (buf.readableBytes() >= 4) {
            System.out.println(buf.readInt());
        }

        //...
        // ChannelHandlerContext ctx = CHANNEL_HANDLER_CONTEXT_FROM_SOMEWHERE; //get reference form somewhere
        // ByteBufAllocator allocator2 = ctx.alloc();
        //...
    }

    /**
     * Listing 5.14 Obtaining a ByteBufAllocator reference
     */
    @Test
    public void bufUtilsTest(){
        Channel channel = CHANNEL_FROM_SOMEWHERE; //get reference form somewhere
        ByteBufAllocator allocator = channel.alloc();
        ByteBuf buf = allocator.buffer(32);

        int count  = 0x12_34_56_78;
        while (buf.writableBytes() >= 4) {
            buf.writeInt(count);
            count++;
        }

        System.out.println(ByteBufUtil.prettyHexDump(buf));
    }

    /**
     * Listing 5.15 Reference counting
     * */
    @Test
    public void referenceCounting(){
        Channel channel = new NioSocketChannel(); //get reference form somewhere
        ByteBufAllocator allocator = channel.alloc();
        ByteBuf buffer = allocator.directBuffer();
        assert buffer.refCnt() == 1;
        //...

        ByteBuf buf = Unpooled.buffer();
        assertEquals(buf.refCnt(), 1);
    }

    /**
     * Listing 5.16 Release reference-counted object
     */
    @Test
    public void bufferRelease(){
        ByteBuf buf = Unpooled.buffer();
        assertEquals(buf.refCnt(), 1);

        buf.retain();
        assertEquals(buf.refCnt(), 2);

        boolean released = buf.release();
        assertFalse(released);
        assertEquals(buf.refCnt(), 1);

        released = buf.release();
        assertTrue(released);
        assertEquals(buf.refCnt(), 0);
        //...
    }

    @Test(expected = io.netty.util.IllegalReferenceCountException.class)
    public void bufferReleaseException(){
        ByteBuf buf = Unpooled.buffer();
        assertEquals(buf.refCnt(), 1);

        boolean released = buf.release();
        assertTrue(released);
        assertEquals(buf.refCnt(), 0); // buf已经被释放了

        // 对已经释放的buf进行任何操作，都会抛出异常
        buf.release();  // throw exception
    }


}