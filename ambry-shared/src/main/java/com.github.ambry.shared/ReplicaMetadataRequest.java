package com.github.ambry.shared;

import com.github.ambry.clustermap.ClusterMap;
import com.github.ambry.store.FindTokenFactory;
import com.github.ambry.utils.Utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Replica metadata request to get new entries for replication
 */
public class ReplicaMetadataRequest extends RequestOrResponse {
  private List<ReplicaMetadataRequestInfo> replicaMetadataRequestInfoList;
  private long maxTotalSizeOfEntriesInBytes;
  private long replicaMetadataRequestInfoListSizeInBytes;

  private static final int Max_Entries_Size_In_Bytes = 8;
  private static final int Replica_Metadata_Request_Info_List_Size_In_Bytes = 4;

  public ReplicaMetadataRequest(int correlationId, String clientId,
      List<ReplicaMetadataRequestInfo> replicaMetadataRequestInfoList, long maxTotalSizeOfEntriesInBytes) {
    super(RequestOrResponseType.ReplicaMetadataRequest, Request_Response_Version, correlationId, clientId);
    if (replicaMetadataRequestInfoList == null) {
      throw new IllegalArgumentException("replicaMetadataRequestInfoList cannot be null");
    }
    this.replicaMetadataRequestInfoList = replicaMetadataRequestInfoList;
    this.maxTotalSizeOfEntriesInBytes = maxTotalSizeOfEntriesInBytes;
    this.replicaMetadataRequestInfoListSizeInBytes = 0;
    for (ReplicaMetadataRequestInfo replicaMetadataRequestInfo : replicaMetadataRequestInfoList) {
      this.replicaMetadataRequestInfoListSizeInBytes += replicaMetadataRequestInfo.sizeInBytes();
    }
  }

  public static ReplicaMetadataRequest readFrom(DataInputStream stream, ClusterMap clusterMap, FindTokenFactory factory)
      throws IOException {
    RequestOrResponseType type = RequestOrResponseType.ReplicaMetadataRequest;
    Short versionId = stream.readShort();
    int correlationId = stream.readInt();
    String clientId = Utils.readIntString(stream);
    int replicaMetadataRequestInfoListCount = stream.readInt();
    ArrayList<ReplicaMetadataRequestInfo> replicaMetadataRequestInfoList =
        new ArrayList<ReplicaMetadataRequestInfo>(replicaMetadataRequestInfoListCount);
    for (int i = 0; i < replicaMetadataRequestInfoListCount; i++) {
      ReplicaMetadataRequestInfo replicaMetadataRequestInfo =
          ReplicaMetadataRequestInfo.readFrom(stream, clusterMap, factory);
      replicaMetadataRequestInfoList.add(replicaMetadataRequestInfo);
    }
    long maxTotalSizeOfEntries = stream.readLong();
    // ignore version for now
    return new ReplicaMetadataRequest(correlationId, clientId, replicaMetadataRequestInfoList, maxTotalSizeOfEntries);
  }

  public List<ReplicaMetadataRequestInfo> getReplicaMetadataRequestInfoList() {
    return replicaMetadataRequestInfoList;
  }

  public long getMaxTotalSizeOfEntriesInBytes() {
    return maxTotalSizeOfEntriesInBytes;
  }

  @Override
  public void writeTo(WritableByteChannel channel)
      throws IOException {
    if (bufferToSend == null) {
      bufferToSend = ByteBuffer.allocate((int) sizeInBytes());
      writeHeader();
      bufferToSend.putInt(replicaMetadataRequestInfoList.size());
      for (ReplicaMetadataRequestInfo replicaMetadataRequestInfo : replicaMetadataRequestInfoList) {
        replicaMetadataRequestInfo.writeTo(bufferToSend);
      }
      bufferToSend.putLong(maxTotalSizeOfEntriesInBytes);
      bufferToSend.flip();
    }
    if (bufferToSend.remaining() > 0) {
      channel.write(bufferToSend);
    }
  }

  @Override
  public boolean isSendComplete() {
    return bufferToSend != null && bufferToSend.remaining() == 0;
  }

  @Override
  public long sizeInBytes() {
    return super.sizeInBytes() + Replica_Metadata_Request_Info_List_Size_In_Bytes +
        replicaMetadataRequestInfoListSizeInBytes + Max_Entries_Size_In_Bytes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ReplicaMetadataRequest[");
    for (ReplicaMetadataRequestInfo replicaMetadataRequestInfo : replicaMetadataRequestInfoList) {
      sb.append(replicaMetadataRequestInfo.toString());
    }
    sb.append(", ").append("maxTotalSizeOfEntriesInBytes=").append(maxTotalSizeOfEntriesInBytes);
    sb.append("]");
    return sb.toString();
  }
}
