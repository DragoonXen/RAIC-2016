import model.*;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class RemoteProcessClient implements Closeable {
    private static final int BUFFER_SIZE_BYTES = 1 << 20;
    private static final ByteOrder PROTOCOL_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final int INTEGER_SIZE_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int LONG_SIZE_BYTES = Long.SIZE / Byte.SIZE;

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final ByteArrayOutputStream outputStreamBuffer;

    private Tree[] previousTrees;

    public RemoteProcessClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setSendBufferSize(BUFFER_SIZE_BYTES);
        socket.setReceiveBufferSize(BUFFER_SIZE_BYTES);
        socket.setTcpNoDelay(true);

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        outputStreamBuffer = new ByteArrayOutputStream(BUFFER_SIZE_BYTES);
    }

    public void writeToken(String token) throws IOException {
        writeEnum(MessageType.AUTHENTICATION_TOKEN);
        writeString(token);
        flush();
    }

    public void writeProtocolVersion() throws IOException {
        writeEnum(MessageType.PROTOCOL_VERSION);
        writeInt(1);
        flush();
    }

    public int readTeamSize() throws IOException {
        ensureMessageType(readEnum(MessageType.class), MessageType.TEAM_SIZE);
        return readInt();
    }

    public Game readGameContext() throws IOException {
        ensureMessageType(readEnum(MessageType.class), MessageType.GAME_CONTEXT);
        if (!readBoolean()) {
            return null;
        }

        return new Game(
                readLong(), readInt(), readDouble(), readBoolean(), readBoolean(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readInt(),
                readDouble(), readInt(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readDouble(), readDouble(), readInt(), readInt(), readInt(), readInt(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readInt(), readInt(), readInt(), readInt(), readInt(),
                readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(),
                readInt(), readDouble(), readDouble(), readIntArray(), readDouble(), readDouble(), readDouble(),
                readDouble(), readInt(), readInt(), readInt(), readInt(), readDouble(), readDouble(), readInt(),
                readDouble(), readDouble(), readDouble(), readInt(), readInt(), readDouble(), readDouble(), readInt(),
                readDouble(), readDouble(), readInt(), readDouble(), readDouble(), readInt(), readDouble(),
                readDouble(), readDouble(), readDouble(), readInt(), readInt(), readDouble(), readDouble(),
                readDouble(), readDouble(), readInt(), readInt(), readDouble(), readDouble(), readDouble(),
                readDouble(), readInt(), readInt(), readInt(), readInt(), readInt(), readDouble(), readInt(), readInt(),
                readDouble(), readDouble(), readDouble(), readInt(), readDouble(), readDouble(), readDouble(),
                readDouble(), readInt(), readInt(), readDouble(), readInt()
        );
    }

    public PlayerContext readPlayerContext() throws IOException {
        MessageType messageType = readEnum(MessageType.class);
        if (messageType == MessageType.GAME_OVER) {
            return null;
        }

        ensureMessageType(messageType, MessageType.PLAYER_CONTEXT);
        return readBoolean() ? new PlayerContext(readWizards(), readWorld()) : null;
    }

    public void writeMoves(Move[] moves) throws IOException {
        writeEnum(MessageType.MOVES);
        writeArray(moves, this::writeMove);
        flush();
    }

    private void writeMove(Move move) throws IOException {
        if (move == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);

            writeDouble(move.getSpeed());
            writeDouble(move.getStrafeSpeed());
            writeDouble(move.getTurn());
            writeEnum(move.getAction());
            writeDouble(move.getCastAngle());
            writeDouble(move.getMinCastDistance());
            writeDouble(move.getMaxCastDistance());
            writeLong(move.getStatusTargetId());
            writeEnum(move.getSkillToLearn());
            writeMessages(move.getMessages());
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private World readWorld() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new World(
                readInt(), readInt(), readDouble(), readDouble(), readPlayers(), readWizards(), readMinions(),
                readProjectiles(), readBonuses(), readBuildings(), readTrees()
        );
    }

    private Player[] readPlayers() throws IOException {
        return readArray(Player.class, this::readPlayer);
    }

    private Player readPlayer() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Player(readLong(), readBoolean(), readString(), readBoolean(), readInt(), readEnum(Faction.class));
    }

    private Wizard[] readWizards() throws IOException {
        return readArray(Wizard.class, this::readWizard);
    }

    private Wizard readWizard() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Wizard(
                readLong(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readEnum(Faction.class), readDouble(), readInt(), readInt(), readStatuses(), readLong(), readBoolean(),
                readInt(), readInt(), readDouble(), readDouble(), readInt(), readInt(), readEnumArray(SkillType.class),
                readInt(), readIntArray(), readBoolean(), readMessages()
        );
    }

    private Minion[] readMinions() throws IOException {
        return readArray(Minion.class, this::readMinion);
    }

    private Minion readMinion() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Minion(
                readLong(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readEnum(Faction.class), readDouble(), readInt(), readInt(), readStatuses(), readEnum(MinionType.class),
                readDouble(), readInt(), readInt(), readInt()
        );
    }

    private Projectile[] readProjectiles() throws IOException {
        return readArray(Projectile.class, this::readProjectile);
    }

    private Projectile readProjectile() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Projectile(
                readLong(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readEnum(Faction.class), readDouble(), readEnum(ProjectileType.class), readLong(), readLong()
        );
    }

    private Bonus[] readBonuses() throws IOException {
        return readArray(Bonus.class, this::readBonus);
    }

    private Bonus readBonus() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Bonus(
                readLong(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readEnum(Faction.class), readDouble(), readEnum(BonusType.class)
        );
    }

    private Building[] readBuildings() throws IOException {
        return readArray(Building.class, this::readBuilding);
    }

    private Building readBuilding() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Building(
                readLong(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readEnum(Faction.class), readDouble(), readInt(), readInt(), readStatuses(),
                readEnum(BuildingType.class), readDouble(), readDouble(), readInt(), readInt(), readInt()
        );
    }

    private Tree[] readTrees() throws IOException {
        Tree[] trees = readArray(Tree.class, this::readTree);
        return trees == null ? previousTrees : (previousTrees = trees);
    }

    private Tree readTree() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Tree(
                readLong(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readEnum(Faction.class), readDouble(), readInt(), readInt(), readStatuses()
        );
    }

    private Message[] readMessages() throws IOException {
        return readArray(Message.class, this::readMessage);
    }

    private Message readMessage() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Message(readEnum(LaneType.class), readEnum(SkillType.class), readByteArray(false));
    }

    private void writeMessages(Message[] messages) throws IOException {
        writeArray(messages, this::writeMessage);
    }

    private void writeMessage(Message message) throws IOException {
        if (message == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);

            writeEnum(message.getLane());
            writeEnum(message.getSkillToLearn());
            writeByteArray(message.getRawMessage());
        }
    }

    private Status[] readStatuses() throws IOException {
        return readArray(Status.class, this::readStatus);
    }

    private Status readStatus() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Status(readLong(), readEnum(StatusType.class), readLong(), readLong(), readInt());
    }

    private static void ensureMessageType(MessageType actualType, MessageType expectedType) {
        if (actualType != expectedType) {
            throw new IllegalArgumentException(String.format(
                    "Received wrong message [actual=%s, expected=%s].", actualType, expectedType
            ));
        }
    }

    private <E> E[] readArray(Class<E> elementClass, ElementReader<E> elementReader) throws IOException {
        int length = readInt();
        if (length < 0) {
            return null;
        }

        @SuppressWarnings("unchecked") E[] array = (E[]) Array.newInstance(elementClass, length);

        for (int i = 0; i < length; ++i) {
            array[i] = elementReader.read();
        }

        return array;
    }

    private <E> void writeArray(E[] array, ElementWriter<E> elementWriter) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            int length = array.length;
            writeInt(length);

            for (int i = 0; i < length; ++i) {
                elementWriter.write(array[i]);
            }
        }
    }

    private byte[] readByteArray(boolean nullable) throws IOException {
        int count = readInt();

        if (nullable) {
            if (count < 0) {
                return null;
            }
        } else {
            if (count <= 0) {
                return EMPTY_BYTE_ARRAY;
            }
        }

        return readBytes(count);
    }

    private void writeByteArray(byte[] array) throws IOException {
        if (array == null) {
            writeInt(-1);
        } else {
            writeInt(array.length);
            writeBytes(array);
        }
    }

    private <E extends Enum> E readEnum(Class<E> enumClass) throws IOException {
        byte ordinal = readBytes(1)[0];

        E[] values = enumClass.getEnumConstants();
        int valueCount = values.length;

        for (int valueIndex = 0; valueIndex < valueCount; ++valueIndex) {
            E value = values[valueIndex];
            if (value.ordinal() == ordinal) {
                return value;
            }
        }

        return null;
    }

    @SuppressWarnings("SubtractionInCompareTo")
    private <E extends Enum> E[] readEnumArray(Class<E> enumClass, int count) throws IOException {
        byte[] bytes = readBytes(count);
        @SuppressWarnings("unchecked") E[] array = (E[]) Array.newInstance(enumClass, count);

        E[] values = enumClass.getEnumConstants();
        int valueCount = values.length;

        Arrays.sort(values, (valueA, valueB) -> valueA.ordinal() - valueB.ordinal());

        for (int i = 0; i < count; ++i) {
            byte ordinal = bytes[i];

            if (ordinal >= 0 && ordinal < valueCount) {
                array[i] = values[ordinal];
            }
        }

        return array;
    }

    private <E extends Enum> E[] readEnumArray(Class<E> enumClass) throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        return readEnumArray(enumClass, count);
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum> E[][] readEnumArray2D(Class<E> enumClass) throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        E[][] array;
        try {
            array = (E[][]) Array.newInstance(Class.forName("[L" + enumClass.getName() + ';'), count);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Can't load array class for " + enumClass + '.', e);
        }

        for (int i = 0; i < count; ++i) {
            array[i] = readEnumArray(enumClass);
        }

        return array;
    }

    private <E extends Enum> void writeEnum(E value) throws IOException {
        writeByte(value == null ? -1 : value.ordinal());
    }

    private String readString() throws IOException {
        int length = readInt();
        if (length == -1) {
            return null;
        }

        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    private void writeString(String value) throws IOException {
        if (value == null) {
            writeInt(-1);
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        writeInt(bytes.length);
        writeBytes(bytes);
    }

    private boolean readBoolean() throws IOException {
        return readBytes(1)[0] != 0;
    }

    private boolean[] readBooleanArray(int count) throws IOException {
        byte[] bytes = readBytes(count);
        boolean[] array = new boolean[count];

        for (int i = 0; i < count; ++i) {
            array[i] = bytes[i] != 0;
        }

        return array;
    }

    private boolean[] readBooleanArray() throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        return readBooleanArray(count);
    }

    private boolean[][] readBooleanArray2D() throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        boolean[][] array = new boolean[count][];

        for (int i = 0; i < count; ++i) {
            array[i] = readBooleanArray();
        }

        return array;
    }

    private void writeBoolean(boolean value) throws IOException {
        writeByte(value ? 1 : 0);
    }

    private int readInt() throws IOException {
        return ByteBuffer.wrap(readBytes(INTEGER_SIZE_BYTES)).order(PROTOCOL_BYTE_ORDER).getInt();
    }

    private int[] readIntArray(int count) throws IOException {
        byte[] bytes = readBytes(count * INTEGER_SIZE_BYTES);
        int[] array = new int[count];

        for (int i = 0; i < count; ++i) {
            array[i] = ByteBuffer.wrap(
                    bytes, i * INTEGER_SIZE_BYTES, INTEGER_SIZE_BYTES
            ).order(PROTOCOL_BYTE_ORDER).getInt();
        }

        return array;
    }

    private int[] readIntArray() throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        return readIntArray(count);
    }

    private int[][] readIntArray2D() throws IOException {
        int count = readInt();
        if (count < 0) {
            return null;
        }

        int[][] array = new int[count][];

        for (int i = 0; i < count; ++i) {
            array[i] = readIntArray();
        }

        return array;
    }

    private void writeInt(int value) throws IOException {
        writeBytes(ByteBuffer.allocate(INTEGER_SIZE_BYTES).order(PROTOCOL_BYTE_ORDER).putInt(value).array());
    }

    private long readLong() throws IOException {
        return ByteBuffer.wrap(readBytes(LONG_SIZE_BYTES)).order(PROTOCOL_BYTE_ORDER).getLong();
    }

    private void writeLong(long value) throws IOException {
        writeBytes(ByteBuffer.allocate(LONG_SIZE_BYTES).order(PROTOCOL_BYTE_ORDER).putLong(value).array());
    }

    private double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    private void writeDouble(double value) throws IOException {
        writeLong(Double.doubleToLongBits(value));
    }

    private byte[] readBytes(int byteCount) throws IOException {
        byte[] bytes = new byte[byteCount];
        int offset = 0;
        int readByteCount;

        while (offset < byteCount && (readByteCount = inputStream.read(bytes, offset, byteCount - offset)) != -1) {
            offset += readByteCount;
        }

        if (offset != byteCount) {
            throw new IOException(String.format("Can't read %d bytes from input stream.", byteCount));
        }

        return bytes;
    }

    private void writeBytes(byte[] bytes) throws IOException {
        outputStreamBuffer.write(bytes);
    }

    private void writeByte(int value) throws IOException {
        try {
            outputStreamBuffer.write(value);
        } catch (RuntimeException e) {
            throw new IOException("Can't write a byte into output stream.", e);
        }
    }

    private void flush() throws IOException {
        outputStream.write(outputStreamBuffer.toByteArray());
        outputStreamBuffer.reset();
        outputStream.flush();
    }

    private interface ElementReader<E> {
        E read() throws IOException;
    }

    private interface ElementWriter<E> {
        void write(E element) throws IOException;
    }

    @SuppressWarnings("unused")
    private enum MessageType {
        UNKNOWN,
        GAME_OVER,
        AUTHENTICATION_TOKEN,
        TEAM_SIZE,
        PROTOCOL_VERSION,
        GAME_CONTEXT,
        PLAYER_CONTEXT,
        MOVES
    }
}
