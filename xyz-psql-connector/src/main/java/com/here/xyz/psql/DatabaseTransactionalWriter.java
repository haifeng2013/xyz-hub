package com.here.xyz.psql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ObjectArrays;
import com.here.xyz.models.geojson.implementation.Feature;
import com.here.xyz.models.geojson.implementation.FeatureCollection;
import com.here.xyz.models.geojson.implementation.Geometry;
import com.vividsolutions.jts.io.WKBWriter;
import org.postgresql.util.PGobject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseTransactionalWriter extends  DatabaseWriter{

    public static FeatureCollection insertFeatures(String schema, String table, String streamId, FeatureCollection collection,
                                                   List<Feature> inserts, Connection connection)
            throws SQLException, JsonProcessingException {

        boolean batchInsert = false;
        boolean batchInsertWithoutGeometry = false;

        final PreparedStatement insertStmt = createInsertStatement(connection,schema,table);
        final PreparedStatement insertWithoutGeometryStmt = createInsertWithoutGeometryStatement(connection,schema,table);

        for (int i = 0; i < inserts.size(); i++) {
            final Feature feature = inserts.get(i);

            final PGobject jsonbObject= featureToPGobject(feature, true);
            final PGobject geojsonbObject = featureToPGobject(feature, false);

            if (feature.getGeometry() == null) {
                insertWithoutGeometryStmt.setObject(1, jsonbObject);
                insertWithoutGeometryStmt.addBatch();
                batchInsertWithoutGeometry = true;
            } else {
                insertStmt.setObject(1, jsonbObject);
                final WKBWriter wkbWriter = new WKBWriter(3);
                insertStmt.setBytes(2, wkbWriter.write(feature.getGeometry().getJTSGeometry()));
                insertStmt.setObject(3, geojsonbObject);

                insertStmt.addBatch();
                batchInsert = true;
            }
            collection.getFeatures().add(feature);
        }

        if (batchInsert) {
            insertStmt.executeBatch();
        }
        if (batchInsertWithoutGeometry) {
            insertWithoutGeometryStmt.executeBatch();
        }

        return collection;
    }

    public static FeatureCollection updateFeatures(String schema, String table, String streamId, FeatureCollection collection,
                                                   List<FeatureCollection.ModificationFailure> fails, List<Feature> updates,
                                                   Connection connection, boolean handleUUID)
            throws SQLException, JsonProcessingException {

        final PreparedStatement updateStmt = createUpdateStatement(connection, schema, table, handleUUID);
        final PreparedStatement updateWithoutGeometryStmt = createUpdateWithoutGeometryStatement(connection,schema,table,handleUUID);

        List<String> updateIdList = new ArrayList<>();
        List<String> updateWithoutGeometryIdList = new ArrayList<>();

        int[] batchUpdateResult = null;
        int[] batchUpdateWithoutGeometryResult = null;

        for (int i = 0; i < updates.size(); i++) {
            final Feature feature = updates.get(i);
            final String puuid = feature.getProperties().getXyzNamespace().getPuuid();

            if (feature.getId() == null) {
                throw new NullPointerException("id");
            }

            final PGobject jsonbObject= featureToPGobject(feature, true);
            final PGobject geojsonbObject = featureToPGobject(feature, false);

            if (feature.getGeometry() == null) {
                updateWithoutGeometryStmt.setObject(1, jsonbObject);
                updateWithoutGeometryStmt.setString(2, feature.getId());
                if(handleUUID)
                    updateWithoutGeometryStmt.setString(3, puuid);
                updateWithoutGeometryStmt.addBatch();

                updateWithoutGeometryIdList.add(feature.getId());
            } else {
                updateStmt.setObject(1, jsonbObject);
                final WKBWriter wkbWriter = new WKBWriter(3);
                updateStmt.setBytes(2, wkbWriter.write(feature.getGeometry().getJTSGeometry()));
                updateStmt.setObject(3, geojsonbObject);
                updateStmt.setString(4, feature.getId());
                if(handleUUID) {
                    updateStmt.setString(5, puuid);
                }
                updateStmt.addBatch();

                updateIdList.add(feature.getId());
            }
            collection.getFeatures().add(feature);
        }

        if (updateIdList.size() > 0) {
            batchUpdateResult = updateStmt.executeBatch();
            if(batchUpdateResult.length != updateIdList.size())
                throw new SQLException("Couldn't update all Objects");
            fillFailList(batchUpdateResult, fails, updateIdList, handleUUID);
        }
        if (updateWithoutGeometryIdList.size() > 0) {
            batchUpdateWithoutGeometryResult = updateWithoutGeometryStmt.executeBatch();
            if(batchUpdateWithoutGeometryResult.length != updateWithoutGeometryIdList.size())
                throw new SQLException("Couldn't update all Objects");
            fillFailList(batchUpdateWithoutGeometryResult, fails, updateWithoutGeometryIdList, handleUUID);
        }

        if(fails.size() > 0)
            throw new SQLException("Updated has failed!");

        return collection;
    }

    protected static void deleteFeatures(String schema, String table, String streamId,
                                         List<FeatureCollection.ModificationFailure> fails, Map<String, String> deletes,
                                         Connection connection, boolean handleUUID)
            throws SQLException {

        final PreparedStatement batchDeleteStmt = deleteStmtSQLStatement(connection,schema,table,handleUUID);
        Set<String> idsToDelete = deletes.keySet();

        for (String deleteId : idsToDelete) {
            final String puuid = deletes.get(deleteId);
            int rows = 0;

            batchDeleteStmt.setString(1, deleteId);
            if(handleUUID) {
                batchDeleteStmt.setString(2, puuid);
            }
            batchDeleteStmt.addBatch();
        }

        int[] batchDeleteStmtResult = batchDeleteStmt.executeBatch();

        fillFailList(batchDeleteStmtResult, fails, new ArrayList<>(idsToDelete), handleUUID);

        if(fails.size() > 0)
            throw new SQLException("Delete has failed!");
    }

    private static void fillFailList(int[] batchResult, List<FeatureCollection.ModificationFailure> fails,  List<String> idList, boolean handleUUID){
        for (int i= 0; i < batchResult.length; i++) {
            if(batchResult[i] == 0 ) {
                fails.add(new FeatureCollection.ModificationFailure().withId(idList.get(i)).withMessage("Object does not exist"+
                    (handleUUID ? " or UUID mismatch" : "" )));
            }
        }
    }
}
