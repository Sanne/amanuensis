package net.dataforte.infinispan.amanuensis.backend.lucene;

import java.util.concurrent.ExecutionException;

import net.dataforte.commons.collections.Computable;
import net.dataforte.commons.collections.Memoizer;
import net.dataforte.commons.slf4j.LoggerFactory;
import net.dataforte.infinispan.amanuensis.AmanuensisManager;
import net.dataforte.infinispan.amanuensis.ExecutorContext;
import net.dataforte.infinispan.amanuensis.IndexOperations;
import net.dataforte.infinispan.amanuensis.IndexerException;
import net.dataforte.infinispan.amanuensis.OperationDispatcher;

import org.slf4j.Logger;

public class LuceneOperationDispatcher implements OperationDispatcher {
	private static final Logger log = LoggerFactory.make();
	private AmanuensisManager manager;
	private Memoizer<String, ExecutorContext> executorContexts;

	public LuceneOperationDispatcher(AmanuensisManager manager) {
		this.manager = manager;		
		this.executorContexts = new Memoizer<String, ExecutorContext>(new ExecutorContextComputer());
	}

	@Override
	public void dispatch(IndexOperations ops) throws IndexerException {
		try {
			ExecutorContext context = executorContexts.compute(ops.getIndexName());
			DirectoryOperationQueueExecutor queueExecutor = new DirectoryOperationQueueExecutor(context, ops);
			context.getExecutor().execute(queueExecutor);
		} catch (Exception e) {
			log.error("", e);
		}
	}
	
	private class ExecutorContextComputer implements Computable<String, ExecutorContext> {
		@Override
		public ExecutorContext compute(String indexName) throws InterruptedException, ExecutionException {
			ExecutorContext executorContext = new ExecutorContext(LuceneOperationDispatcher.this.manager.getDirectoryByIndexName(indexName), LuceneOperationDispatcher.this.manager.getAnalyzer());
			return executorContext;			
		}
		
	}

}