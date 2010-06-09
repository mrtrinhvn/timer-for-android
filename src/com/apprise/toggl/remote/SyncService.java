/**
 * Sync ideology:
 * 
 * (1) retrieve list of all entries from remote server
 * for all records:
 *     (2) fetch local db entry
 *     local entry found?
 *         (2.1) no:
 *                 a: entry is in the removed table => delete from remote
 *                 b: entry is not in removed table => save locally
 *         (2.2) yes: check if local entry is sync dirty?
 *                 dirty: upload local changes to remote
 *                 clean: updating local
 *     (5) remember message as processed
 * (6) for all unprocessed local items
 *     a: skip already processed from server
 *     b: found in local db:
 *         dirty: create entry on server
 *         clean: deleting locally
 */

package com.apprise.toggl.remote;

import java.util.LinkedList;
import java.util.List;

import com.apprise.toggl.Toggl;
import com.apprise.toggl.storage.DatabaseAdapter;
import com.apprise.toggl.storage.DatabaseAdapter.ORM;
import com.apprise.toggl.storage.models.Client;
import com.apprise.toggl.storage.models.DeletedModel;
import com.apprise.toggl.storage.models.Model;
import com.apprise.toggl.storage.models.Project;
import com.apprise.toggl.storage.models.Task;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.util.Log;

public class SyncService extends Service {

  public static final String SYNC_COMPLETED = "com.apprise.toggl.remote.SYNC_COMPLETED";
  public static final String COLLECTION = "com.apprise.toggl.remote.COLLECTION";
  public static final String ALL_COMPLETED = "com.apprise.toggl.remote.ALL_COMPLETED";
  public static final String PROJECTS_COMPLETED = "com.apprise.toggl.remote.PROJECTS_COMPLETED";
  public static final String TASKS_COMPLETED = "com.apprise.toggl.remote.TASKS_COMPLETED";
  public static final String CLIENTS_COMPLETED = "com.apprise.toggl.remote.CLIENTS_COMPLETED";
  
  public static final String TAG = "SyncService";
  
  private Toggl app;
  private TogglWebApi api;
  private DatabaseAdapter dbAdapter;
  
  @Override
  public void onCreate() {
    super.onCreate();
    app = (Toggl) getApplication();
    api = new TogglWebApi(app.getAPIToken());
    dbAdapter = new DatabaseAdapter(this, app);
    dbAdapter.open();
  }
  
  @Override
  public void onDestroy() {
    dbAdapter.close();
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return new SyncBinder(this);
  }
  
  public static class SyncBinder extends Binder {

    private final SyncService service;

    public SyncBinder(SyncService service) {
      this.service = service;
    }

    public SyncService getService() {
      return service;
    }

  }
  
  public void setApiToken(String apiToken) {
    api.setApiToken(apiToken);
  }
  
  public void syncAll() {
    syncProjects();
//    syncClients(); TODO Gson throws errors     
    syncTasks();    
    
    Intent intent = new Intent(SYNC_COMPLETED);
    intent.putExtra(COLLECTION, ALL_COMPLETED);
    sendBroadcast(intent);    
  }
  
  public void syncTasks() {
    Log.d(TAG, "#syncTasks starting to sync.");

    sync(dbAdapter.findAllTasks(), api.fetchTasks(), new SyncProxy() {
      
      public void updateRemoteEntry(Model model) {

      }
      
      public void updateLocalEntry(Model model) {
        dbAdapter.updateTask((Task) model);
      }
      
      public Model getLocalEntry(long remoteId) {
        return dbAdapter.findTaskByRemoteId(remoteId);
      }
      
      public DeletedModel getLocalDeletedEntry(long remoteId) {
        return dbAdapter.findDeletedTask(remoteId);
      }
      
      public void deleteRemoteEntry(long id) {
        
      }
      
      public void deleteLocalEntry(long _id) {
        dbAdapter.deleteTaskHard(_id);
      }
      
      public void deleteLocalDeletedEntry(long _id) {
        dbAdapter.deleteDeletedTask(_id);
      }
      
      public void createRemoteEntry(Model model) {
        
      }
      
      public Model createLocalEntry(Model model) {
        return dbAdapter.createTask((Task) model);
      }
      
      public Model mapEntryFromCursor(Cursor cursor) {
        return ORM.mapTask(cursor, dbAdapter);
      }
      
      public void broadcastSyncCompleted() {
        Intent intent = new Intent(SYNC_COMPLETED);
        intent.putExtra(COLLECTION, TASKS_COMPLETED);
        sendBroadcast(intent);
      }          

    });
  }
  
  public void syncProjects() {
    Log.d(TAG, "#syncProjects starting to sync.");
    
    sync(dbAdapter.findAllProjects(), api.fetchProjects(), new SyncProxy() {
      
      public void updateRemoteEntry(Model model) {
        
      }
      
      public void updateLocalEntry(Model model) {
        dbAdapter.updateProject((Project) model);
      }
      
      public Model getLocalEntry(long remoteId) {
        return dbAdapter.findProjectByRemoteId(remoteId);
      }
      
      public DeletedModel getLocalDeletedEntry(long remoteId) {
        return null;
      }
      
      public void deleteRemoteEntry(long id) { }
      
      public void deleteLocalEntry(long _id) {
        dbAdapter.deleteProject(_id);
      }
      
      public void deleteLocalDeletedEntry(long _id) { }
      
      public void createRemoteEntry(Model model) {
        // TODO
      }
      
      public Model createLocalEntry(Model model) {
        Log.d(TAG, "creating local entry: " + ((Project) model).client_project_name);
        return dbAdapter.createProject((Project) model);
      }
      
      public Model mapEntryFromCursor(Cursor cursor) {
        return ORM.mapProject(cursor, dbAdapter);
      }
      
      public void broadcastSyncCompleted() {
        Intent intent = new Intent(SYNC_COMPLETED);
        intent.putExtra(COLLECTION, PROJECTS_COMPLETED);
        sendBroadcast(intent);
      }      
      
    });
  }
  
  public void syncClients() {
    Log.d(TAG, "#syncClients starting to sync.");
    
    sync(dbAdapter.findAllClients(), api.fetchClients(), new SyncProxy() {
      
      public void updateRemoteEntry(Model model) {

      }
      
      public void updateLocalEntry(Model model) {
        dbAdapter.updateClient((Client) model);
      }
      
      public Model getLocalEntry(long remoteId) {
        return dbAdapter.findClientByRemoteId(remoteId);
      }
      
      public DeletedModel getLocalDeletedEntry(long remoteId) {
        return null;
      }
      
      public void deleteRemoteEntry(long id) { }
      
      public void deleteLocalEntry(long _id) {
        dbAdapter.deleteClient(_id);
      }
      
      public void deleteLocalDeletedEntry(long _id) { }
      
      public void createRemoteEntry(Model model) { }
      
      public Model createLocalEntry(Model model) {
        return dbAdapter.createClient((Client) model);
      }
      
      public Model mapEntryFromCursor(Cursor cursor) {
        return ORM.mapClient(cursor, dbAdapter);
      }
      
      public void broadcastSyncCompleted() {
        Intent intent = new Intent(SYNC_COMPLETED);
        intent.putExtra(COLLECTION, CLIENTS_COMPLETED);
        sendBroadcast(intent);
      }      
      
    });
  }
  
  public void sync(Cursor localCursor, List<? extends Model> remoteEntries, SyncProxy proxy) {
    LinkedList<Long> processedEntries = new LinkedList<Long>();
    
    for(Model remoteEntry : remoteEntries) {
      Model localEntry = proxy.getLocalEntry(remoteEntry.id);
      
      if(localEntry == null) { // 2.1: local entry not found
        DeletedModel deletedEntry = proxy.getLocalDeletedEntry(remoteEntry.id); 
        
        if(deletedEntry != null) { // entry has been deleted locally
          proxy.deleteRemoteEntry(remoteEntry.id);
          proxy.deleteLocalDeletedEntry(deletedEntry._id);
        }
        else { // entry does not exist, nor is deleted
          localEntry = proxy.createLocalEntry(remoteEntry);
          processedEntries.add(localEntry._id);
        }
      }
      else { // 2.2: local entry found
        if(localEntry.sync_dirty) {
          proxy.updateRemoteEntry(localEntry);

          localEntry.sync_dirty = false;
          proxy.updateLocalEntry(localEntry);
        }
        else {
          if(!localEntry.identicalTo(remoteEntry)) {
            // remote entry has changed, updating local entry
            localEntry.updateAttributes(remoteEntry);
            proxy.updateLocalEntry(localEntry);
          }
        }
        processedEntries.add(localEntry._id);
      }
    }
    
    // iterate all local entries
    while(localCursor.moveToNext()) {
      // only id for efficiency
      long _id = localCursor.getLong(localCursor.getColumnIndex(BaseColumns._ID));

      // only process entries that has not yet been processed
      if(!processedEntries.contains(_id)) {
        // two options: either deleted from server, or created locally
        Model localEntry = proxy.mapEntryFromCursor(localCursor);
        
        if(localEntry.sync_dirty) { // created locally
          proxy.createRemoteEntry(localEntry);
          localEntry.sync_dirty = false;
          proxy.updateLocalEntry(localEntry);
        }
        else { // deleted on server 
          proxy.deleteLocalEntry(_id);
        }
      }
    }

    localCursor.close();
    proxy.broadcastSyncCompleted();
  }
  
}

