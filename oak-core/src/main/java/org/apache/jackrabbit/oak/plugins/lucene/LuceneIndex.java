/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.jackrabbit.oak.spi.Cursor;
import org.apache.jackrabbit.oak.spi.Filter;
import org.apache.jackrabbit.oak.spi.Filter.PropertyRestriction;
import org.apache.jackrabbit.oak.spi.QueryIndex;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

public class LuceneIndex implements QueryIndex {

    private final NodeStore store;

    private final String[] path;

    public LuceneIndex(NodeStore store, String... path) {
        this.store = store;
        this.path = path;
    }

    @Override
    public String getIndexName() {
        return "lucene";
    }

    @Override
    public double getCost(Filter filter) {
        return 1.0;
    }

    @Override
    public String getPlan(Filter filter) {
        return getQuery(filter).toString();
    }

    @Override
    public Cursor query(Filter filter, String revisionId) {
        try {
            Directory directory =
                    new OakDirectory(store, store.getRoot(), path);
            try {
                IndexReader reader = IndexReader.open(directory);
                try {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    try {
                        Collection<String> paths = new ArrayList<String>();

                        Query query = getQuery(filter);
                        if (query != null) {
                            TopDocs docs = searcher.search(query, Integer.MAX_VALUE);
                            for (ScoreDoc doc : docs.scoreDocs) {
                                String path = reader.document(doc.doc).get(":path");
                                if ("".equals(path)) {
                                    paths.add("/");
                                } else if (path != null) {
                                    paths.add(path);
                                }
                            }
                        }

                        return new PathCursor(paths);
                    } finally {
                        searcher.close();
                    }
                } finally {
                    reader.close();
                }
            } finally {
                directory.close();
            }
        } catch (IOException e) {
            return new PathCursor(Collections.<String>emptySet());
        }
    }

    private static Query getQuery(Filter filter) {
        List<Query> qs = new ArrayList<Query>();

        String path = filter.getPath();
        if (path.equals("/")) {
            path = "";
        }
        switch (filter.getPathRestriction()) {
        case ALL_CHILDREN:
            qs.add(new PrefixQuery(new Term(":path", path + "/")));
            break;
        case DIRECT_CHILDREN:
            qs.add(new PrefixQuery(new Term(":path", path + "/"))); // FIXME
            break;
        case EXACT:
            qs.add(new TermQuery(new Term(":path", path)));
            break;
        case PARENT:
            int slash = path.lastIndexOf('/');
            if (slash != -1) {
                String parent = path.substring(0, slash);
                qs.add(new TermQuery(new Term(":path", parent)));
            } else {
                return null; // there's no parent of the root node
            }
            break;
        }

        for (PropertyRestriction pr : filter.getPropertyRestrictions()) {
            String name = pr.propertyName;
            String first = pr.first.getString();
            String last = pr.last.getString();
            if (first .equals(last) && pr.firstIncluding && pr.lastIncluding) {
                qs.add(new TermQuery(new Term(name, first)));
            } else {
                qs.add(new TermRangeQuery(
                        name, first, last, pr.firstIncluding, pr.lastIncluding));
            }
        }

        if (qs.size() > 1) {
            BooleanQuery bq = new BooleanQuery();
            for (Query q : qs) {
                bq.add(q, Occur.MUST);
            }
            return bq;
        } else {
            return qs.get(1);
        }
    }

    private static class PathCursor implements Cursor {

        private final Iterator<String> iterator;

        private String path;

        public PathCursor(Collection<String> paths) {
            this.iterator = paths.iterator();
        }

        @Override
        public boolean next() {
            if (iterator.hasNext()) {
                path = iterator.next();
                return true;
            } else {
                path = null;
                return false;
            }
        }

        @Override
        public String currentPath() {
            return path;
        }

    }

}