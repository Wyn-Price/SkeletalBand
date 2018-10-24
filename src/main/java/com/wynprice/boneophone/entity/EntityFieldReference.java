package com.wynprice.boneophone.entity;

import com.wynprice.boneophone.types.MusicianType;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class EntityFieldReference<E extends Entity, T extends MusicianType> {
    protected final Class<E> entityClass;
    protected final String name;
    protected final Predicate<E> entityPredicate;
    protected final Function<E, T> objectGetter;

    @Nullable
    protected T reference;
    @Nullable
    protected UUID entityUUID;

    public EntityFieldReference(Class<E> entityClass, String name, Function<E, T> objectGetter) {
        this(entityClass, name, e -> true, objectGetter);
    }

    public EntityFieldReference(Class<E> entityClass, String name, Predicate<E> entityPredicate, Function<E, T> objectGetter) {
        this.entityClass = entityClass;
        this.name = name;
        this.entityPredicate = entityPredicate;
        this.objectGetter = objectGetter;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public T get(World world) {
        if(this.reference == null && this.entityUUID != null) {
            for (Entity entity : world.loadedEntityList) {
                if(entity.getUniqueID().equals(this.entityUUID) && this.entityClass.isInstance(entity) && this.entityPredicate.test((E) entity)) {
                    this.setReferenceFromEntity((E) entity);
                    return this.reference;
                }
            }
            this.entityUUID = null;
        }
        if(this.reference == null) { //At this point, entityUUID will always be null
            for (Entity entity : world.loadedEntityList) {
                if(this.entityClass.isInstance(entity)
                        && this.entityPredicate.test((E) entity)) {
                    this.setReferenceFromEntity((E) entity);
                    break;
                }
            }
        }
        return this.reference;
    }

    @Nullable
    public T getRawReference() {
        return this.reference;
    }

    public void reset() {
        this.reference = null;
        this.entityUUID = null;
    }

    public void setReferenceFromEntity(@Nonnull E entity) {
        if(this.entityPredicate.test(entity)) {
            this.reference = this.objectGetter.apply(entity);
            this.entityUUID = entity.getUniqueID();
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if(this.entityUUID != null) {
            nbt.setBoolean("Has" + this.name, true);
            nbt.setUniqueId(this.name, this.entityUUID);
        }
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        if(nbt.getBoolean("Has" + this.name)) {
            this.entityUUID = nbt.getUniqueId(this.name);
        }
    }

    public void writeToByteBuf(ByteBuf buf) {
        buf.writeBoolean(this.entityUUID != null);
        if(this.entityUUID != null) {
            buf.writeLong(this.entityUUID.getLeastSignificantBits());
            buf.writeLong(this.entityUUID.getMostSignificantBits());
        }
    }

    public void readFromByteBuf(ByteBuf buf) {
        if(buf.readBoolean()) {
            this.entityUUID = new UUID(buf.readLong(), buf.readLong());
        }
    }
}