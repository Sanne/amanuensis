/**
 * Copyright 2010 Tristan Tarrant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dataforte.infinispan.amanuensis.backend.lucene;

import net.dataforte.commons.slf4j.LoggerFactory;
import net.dataforte.infinispan.amanuensis.ExecutorContext;
import net.dataforte.infinispan.amanuensis.IndexOperation;
import net.dataforte.infinispan.amanuensis.IndexOperations;
import net.dataforte.infinispan.amanuensis.OperationExecutor;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;
import org.slf4j.Logger;

/**
 * 
 * @author Tristan Tarrant
 */
public class DirectoryOperationQueueExecutor implements Runnable {
	private static final Logger log = LoggerFactory.make();

	private ExecutorContext context;
	private IndexOperations ops;

	public DirectoryOperationQueueExecutor(ExecutorContext context, IndexOperations ops) {
		this.context = context;		
		this.ops = ops;
	}
	

	@Override
	public void run() {
		if (this.ops.getOperations().isEmpty()) {
			return;
		}
		try {
			// Obtain an index writer
			IndexWriter writer = context.getWriter();
			for (IndexOperation op : ops.getOperations()) {
				Class<? extends IndexOperation> opClass = op.getClass();

				OperationExecutor<? extends IndexOperation> executor = context.getOperationExecutorFactory().getExecutor(opClass);
				executor.exec(writer, op);
			}
			// Commit the changes
			context.commit();
		} catch (Throwable t) {
			log.error("Error while processing queue for index "+ops.getIndexName(), t);
			try {				
				context.close();
			} finally {				
				if (!(t.getCause() instanceof LockObtainFailedException)) {
					context.forceUnlock();
				}
			}
		}

	}

}
