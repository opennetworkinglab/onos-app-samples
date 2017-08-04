/*
 * Copyright 2015 Open Networking Foundation
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

package org.onosproject.uiref;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Message handler for UI Ref table-view.
 */
public class UiRefTableViewMessageHandler extends UiMessageHandler {

    private static final String UI_REF_TABLE_DATA_REQ = "uiRefTableDataRequest";
    private static final String UI_REF_TABLE_DATA_RESP = "uiRefTableDataResponse";
    private static final String UI_REF_TABLES = "uiRefTables";

    private static final String UI_REF_TABLE_DETAIL_REQ = "uiRefTableDetailsRequest";
    private static final String UI_REF_TABLE_DETAIL_RESP = "uiRefTableDetailsResponse";
    private static final String DETAILS = "details";

    private static final String ID = "id";
    private static final String LABEL = "label";
    private static final String CODE = "code";
    private static final String COMMENT = "comment";
    private static final String RESULT = "result";

    private static final String[] COLUMN_IDS = {ID, LABEL, CODE};

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new UiRefTableDataRequestHandler(),
                new UiRefTableDetailRequestHandler()
        );
    }

    // handler for table view data requests
    private final class UiRefTableDataRequestHandler extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No items found";

        private UiRefTableDataRequestHandler() {
            super(UI_REF_TABLE_DATA_REQ, UI_REF_TABLE_DATA_RESP, UI_REF_TABLES);
        }

        // if necessary, override defaultColumnId() -- if it isn't "id"

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        // if required, override createTableModel() to set
        // column formatters / comparators

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            // === NOTE: the table model supplied here will have been created
            // via  a call to createTableModel(). To assign non-default
            // cell formatters or comparators to the table model, override
            // createTableModel() and set them there.

            // === retrieve table row items from some service... for example:
            // SomeService ss = get(SomeService.class);
            // List<Item> items = ss.getItems()

            // fake data for demonstration purposes...
            List<Item> items = getItems();
            for (Item item: items) {
                populateRow(tm.addRow(), item);
            }
        }

        private void populateRow(TableModel.Row row, Item item) {
            row.cell(ID, item.id())
                    .cell(LABEL, item.label())
                    .cell(CODE, item.code());
        }
    }


    // handler for table view item details requests
    private final class UiRefTableDetailRequestHandler extends RequestHandler {

        private UiRefTableDetailRequestHandler() {
            super(UI_REF_TABLE_DETAIL_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID, "(none)");

            // SomeService ss = get(SomeService.class);
            // Item item = ss.getItemDetails(id)

            // fake data for demonstration purposes...
            Item item = getItem(id);

            ObjectNode rootNode = objectNode();
            ObjectNode data = objectNode();
            rootNode.set(DETAILS, data);

            if (item == null) {
                rootNode.put(RESULT, "Item with id '" + id + "' not found");
                log.warn("attempted to get item detail for id '{}'", id);

            } else {
                rootNode.put(RESULT, "Found item with id '" + id + "'");

                data.put(ID, item.id());
                data.put(LABEL, item.label());
                data.put(CODE, item.code());
                data.put(COMMENT, "Some arbitrary comment");
            }

            sendMessage(UI_REF_TABLE_DETAIL_RESP, rootNode);
        }
    }


    // ===================================================================
    // NOTE: The code below this line is to create fake data for this
    //       sample code. Normally you would use existing services to
    //       provide real data.

    // Lookup a single item.
    private static Item getItem(String id) {
        // We realize this code is really inefficient, but
        // it suffices for our purposes of demonstration...
        for (Item item : getItems()) {
            if (item.id().equals(id)) {
                return item;
            }
        }
        return null;
    }

    // Produce a list of items.
    private static List<Item> getItems() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("item-1", "foo", 42));
        items.add(new Item("item-2", "bar", 99));
        items.add(new Item("item-3", "baz", 65));
        return items;
    }

    // Simple model class to provide sample data
    private static class Item {
        private final String id;
        private final String label;
        private final int code;

        Item(String id, String label, int code) {
            this.id = id;
            this.label = label;
            this.code = code;
        }

        String id() {
            return id;
        }

        String label() {
            return label;
        }

        int code() {
            return code;
        }
    }
}
