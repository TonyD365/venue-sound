package dev.tonyd365.venuesound.block;

/** Stage box: 8 inputs on stage; its multicore snake carries input N to console input (first + N). */
public class StageBoxBlock extends OutletBlock {
    public final int channels;

    public StageBoxBlock(Properties properties) {
        this(8, properties);
    }

    public StageBoxBlock(int channels, Properties properties) {
        super(properties);
        this.channels = channels;
    }
}
