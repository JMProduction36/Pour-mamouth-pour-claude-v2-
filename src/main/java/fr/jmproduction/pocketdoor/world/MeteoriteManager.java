package fr.jmproduction.pocketdoor.world;

import fr.jmproduction.pocketdoor.PocketDoorMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;

/** Server-side falling meteorite made of temporary real blocks. */
public final class MeteoriteManager {
    private static final int RADIUS=8, COOLDOWN=20*15;
    private static final Map<UUID,Integer> COOLDOWNS=new HashMap<>();
    private static final List<Meteorite> ACTIVE=new ArrayList<>();
    private static final Random RANDOM=new Random();
    private static final class Meteorite {
        final ServerLevel level; final ServerPlayer owner; final int targetX,targetZ,groundY;
        int x,z,y; final Map<BlockPos,BlockState> originals=new HashMap<>();
        Meteorite(ServerPlayer o,int tx,int tz,int gy,int sx,int sz,int sy){owner=o;level=o.getLevel();targetX=tx;targetZ=tz;groundY=gy;x=sx;z=sz;y=sy;}
    }
    private MeteoriteManager(){}
    public static void register(){ServerTickEvents.END_SERVER_TICK.register(server->{
        COOLDOWNS.replaceAll((id,v)->Math.max(0,v-1));
        for(int i=ACTIVE.size()-1;i>=0;i--){Meteorite m=ACTIVE.get(i); if(!m.level.hasChunkAt(new BlockPos(m.x,m.y,m.z))) continue;
            restore(m); int dx=Integer.compare(m.targetX,m.x), dz=Integer.compare(m.targetZ,m.z); m.x+=dx; m.z+=dz; m.y--;
            if(m.y-RADIUS<=m.groundY || !m.level.getBlockState(new BlockPos(m.x,m.y-RADIUS-1,m.z)).isAir()){impact(m);ACTIVE.remove(i);} else place(m);
        }
    }); }
    public static void summon(ServerPlayer player){
        UUID id=player.getUUID(); if(COOLDOWNS.getOrDefault(id,0)>0) return;
        Direction d=player.getDirection(); int distance=90+RANDOM.nextInt(21);
        int tx=player.blockPosition().getX()+d.getStepX()*distance, tz=player.blockPosition().getZ()+d.getStepZ()*distance;
        int ground=player.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,tx,tz)-1;
        int sx=tx-d.getStepX()*14, sz=tz-d.getStepZ()*14, sy=ground+RADIUS+distance;
        Meteorite m=new Meteorite(player,tx,tz,ground,sx,sz,sy); COOLDOWNS.put(id,COOLDOWN); place(m); ACTIVE.add(m);
    }
    public static void disconnect(UUID id){for(int i=ACTIVE.size()-1;i>=0;i--)if(ACTIVE.get(i).owner.getUUID().equals(id)){restore(ACTIVE.remove(i));}COOLDOWNS.remove(id);}
    private static void place(Meteorite m){for(int dx=-RADIUS;dx<=RADIUS;dx++)for(int dy=-RADIUS;dy<=RADIUS;dy++)for(int dz=-RADIUS;dz<=RADIUS;dz++){int d=dx*dx+dy*dy+dz*dz;if(d>RADIUS*RADIUS)continue;BlockPos p=new BlockPos(m.x+dx,m.y+dy,m.z+dz);if(!m.level.hasChunkAt(p))continue;m.originals.putIfAbsent(p,m.level.getBlockState(p));BlockState s=d<=36?Blocks.CRYING_OBSIDIAN.defaultBlockState():(d<=100?Blocks.BASALT.defaultBlockState():((Math.abs(dx)+Math.abs(dy)+Math.abs(dz))%11==0?Blocks.GOLD_BLOCK.defaultBlockState():Blocks.BASALT.defaultBlockState()));m.level.setBlock(p,s,3);if(RANDOM.nextInt(8)==0)m.level.sendParticles(ParticleTypes.FLAME,p.getX()+.5,p.getY()+.5,p.getZ()+.5,1,0,0,0,0);}}
    private static void restore(Meteorite m){for(var e:m.originals.entrySet())if(m.level.hasChunkAt(e.getKey()))m.level.setBlock(e.getKey(),e.getValue(),3);m.originals.clear();}
    private static void impact(Meteorite m){m.y=m.groundY+RADIUS;place(m);BlockPos p=new BlockPos(m.x,m.groundY+1,m.z);m.level.playSound(null,p,SoundEvents.GENERIC_EXPLODE,SoundSource.BLOCKS,3f,.65f);m.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,p.getX(),p.getY(),p.getZ(),2,2,1,2,.1);m.level.sendParticles(ParticleTypes.LAVA,p.getX(),p.getY(),p.getZ(),80,3,2,3,.2);Direction f=m.owner.getDirection().getOpposite();m.level.setBlock(new BlockPos(m.x,m.groundY+RADIUS*2+1,m.z),PocketDoorMod.POCKET_DOOR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,f),3);}
}
