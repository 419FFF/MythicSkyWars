package systems.mythical.mythicskywars.nms.v1_8_R3;

import net.minecraft.server.v1_8_R3.BiomeBase;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.EnumCreatureType;
import net.minecraft.server.v1_8_R3.IChunkProvider;
import net.minecraft.server.v1_8_R3.IProgressUpdate;
import net.minecraft.server.v1_8_R3.World;

import java.util.Collections;
import java.util.List;

/**
 * Stand-in generator for a world WindSpigot already unloaded.
 * Unload clears the real generator, then the same tick still calls {@code unloadChunks()}.
 */
final class EmptyChunkProvider implements IChunkProvider {

    static final EmptyChunkProvider INSTANCE = new EmptyChunkProvider();

    private EmptyChunkProvider() {
    }

    @Override
    public boolean isChunkLoaded(int i, int j) {
        return false;
    }

    @Override
    public Chunk getOrCreateChunk(int i, int j) {
        return null;
    }

    @Override
    public Chunk getChunkAt(BlockPosition blockposition) {
        return null;
    }

    @Override
    public void getChunkAt(IChunkProvider ichunkprovider, int i, int j) {
    }

    @Override
    public boolean a(IChunkProvider ichunkprovider, Chunk chunk, int i, int j) {
        return false;
    }

    @Override
    public boolean saveChunks(boolean flag, IProgressUpdate iprogressupdate) {
        return true;
    }

    @Override
    public boolean unloadChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return false;
    }

    @Override
    public String getName() {
        return "empty";
    }

    @Override
    public List<BiomeBase.BiomeMeta> getMobsFor(EnumCreatureType enumcreaturetype, BlockPosition blockposition) {
        return Collections.emptyList();
    }

    @Override
    public BlockPosition findNearestMapFeature(World world, String s, BlockPosition blockposition) {
        return null;
    }

    @Override
    public int getLoadedChunks() {
        return 0;
    }

    @Override
    public void recreateStructures(Chunk chunk, int i, int j) {
    }

    @Override
    public void c() {
    }
}
