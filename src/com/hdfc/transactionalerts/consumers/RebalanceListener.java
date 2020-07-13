package com.hdfc.transactionalerts.consumers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.log4j.Logger;

import com.hdfc.transactionalerts.utils.Utils;

public class RebalanceListener implements ConsumerRebalanceListener {
	
	private static Logger logger = Logger.getLogger(RebalanceListener.class);

	private KafkaConsumer<String, String> consumer;
    private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    public RebalanceListener(KafkaConsumer<String, String> consumer) {
        this.consumer = consumer;
    }

    public void addOffset(String topic, int partition, long offset) {
        currentOffsets.put(new TopicPartition(topic, partition), new OffsetAndMetadata(offset, "Commit"));
    }
    
    public Map<TopicPartition, OffsetAndMetadata> getCurrentOffsets() {
        return currentOffsets;
    }

	public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
    	
        logger.warn("New Partitions Assigned ....");
        for (TopicPartition partition : partitions) {
            logger.warn(String.format("Partition-%d assigned for topic %s", partition.partition(), partition.topic()));
        }
        settingTheCurrentOffset(partitions);
    }

    public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
 
        for (TopicPartition partition : revokedPartitions)
        	logger.warn(String.format("Partition-%d revoked for topic %s", partition.partition(), partition.topic()));

        for (TopicPartition tp : currentOffsets.keySet())
        	logger.info(String.format("Offset for Partition-%d committed", tp.partition()));

        consumer.commitSync(getCurrentOffsets());
        revokedPartitions.forEach(partition -> currentOffsets.remove(partition));
    }
    
    public void settingTheCurrentOffset(Collection<TopicPartition> topicPartitions) {
		
/*		String offsetFileLoc = NotificationConfig.getOffsetFile();
		try {
			//Starting from the last committed offset stored in file
			String offsetJson = new String(Files.readAllBytes(Paths.get(offsetFileLoc))); 
			logger.info("Setting the current offset fetched from Offset File");
//			JSONObject offsetJson = new JSONObject(partitionOffsets);
			for(TopicPartition topicPartition : topicPartitions) {

				String partitionTopic = String.format("%s-%d", topicPartition.topic(), topicPartition.partition());
				int sbIdx = offsetJson.indexOf(partitionTopic);
				if(sbIdx == -1) {
					logger.warn(String.format("Partition %s MetaData not found in file", partitionTopic));
				}
				String offsetAndMetaData = sbIdx == -1 ? null : offsetJson.substring(sbIdx+partitionTopic.length()+1);
				long lastCommittedOffset = getLastCommittedOffset(topicPartition, offsetAndMetaData);
				consumer.seek(topicPartition, lastCommittedOffset + 1);
				addOffset(topicPartition.topic(), topicPartition.partition(), lastCommittedOffset);
				logger.info(String.format("Consumer partition %s-%d has been assigned the offset : %d", topicPartition.topic(), topicPartition.partition(), lastCommittedOffset));
			}
		} catch (IOException e) {
			logger.warn(String.format("Exception occurred while reading the Offset File from Location: %s", offsetFileLoc), e);*/
			logger.info("Setting the current offset fetched from Kafka");
			try {
				for(TopicPartition topicPartition : topicPartitions) {
					long lastCommittedOffset = getLastCommittedOffset(topicPartition, null);
					consumer.seek(topicPartition, lastCommittedOffset + 1);
//					addOffset(topicPartition.topic(), topicPartition.partition(), lastCommittedOffset);
					logger.info(String.format("Consumer partition %s-%d has been assigned the offset : %d", topicPartition.topic(), topicPartition.partition(), lastCommittedOffset+1));
				}
			}catch (Exception x) {
				logger.error("Setting offset from Kafka failed", x);
			}
//		}
	}

    
	private long getLastCommittedOffset(TopicPartition topicPartition, String offsetAndMetaData) {

		if(Utils.isStringNullOrEmpty(offsetAndMetaData)) {
			OffsetAndMetadata offsetMetaData = consumer.committed(topicPartition);
			return offsetMetaData!= null ? offsetMetaData.offset() : -1;
		} else
			return Long.valueOf(offsetAndMetaData.substring(offsetAndMetaData.indexOf("offset=")+7, offsetAndMetaData.indexOf(",")));
	}

}