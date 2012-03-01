/*
 * Copyright (C) 2012 Pixmob (http://github.com/pixmob)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pixmob.hcl.demo;

import static org.pixmob.hcl.demo.Constants.TAG;

import java.util.HashMap;
import java.util.Map;

import org.pixmob.hcl.demo.TaskListFragment.TaskContext;
import org.pixmob.hcl.demo.tasks.DownloadFileTask;
import org.pixmob.hcl.demo.tasks.PostFormTask;
import org.pixmob.hcl.demo.tasks.RedirectTask;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Fragment for displaying task list.
 * @author Pixmob
 */
public class TaskListFragment extends SherlockListFragment implements
        LoaderCallbacks<TaskContext> {
    private TaskContext[] taskContexts = new TaskContext[0];
    private TaskContextAdapter taskContextAdapter;
    private MenuItem startMenuItem;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        final Context context = getActivity().getApplicationContext();
        taskContexts = new TaskContext[] {
                new TaskContext(new DownloadFileTask(context)),
                new TaskContext(new RedirectTask(context)),
                new TaskContext(new PostFormTask(context)) };
        taskContextAdapter = new TaskContextAdapter(getActivity(), taskContexts);
        setListAdapter(taskContextAdapter);
        
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        setSelection(position);
        
        final TaskContext taskContext = taskContextAdapter.getItem(position);
        final String url = taskContext.task.getSourceCodeUrl();
        if (url != null) {
            final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        
        startMenuItem = menu.add(Menu.NONE, R.string.menu_start_demo,
            Menu.NONE, R.string.menu_start_demo);
        startMenuItem.setIcon(R.drawable.ic_menu_play_clip).setShowAsAction(
            MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        menu.add(Menu.NONE, R.string.menu_help, Menu.NONE, R.string.menu_help)
                .setIcon(R.drawable.ic_menu_help)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.menu_start_demo:
                onMenuStart();
                break;
            case R.string.menu_help:
                onMenuHelp();
                break;
            default:
                return false;
        }
        return true;
    }
    
    private void onMenuStart() {
        startMenuItem.setVisible(false);
        getLoaderManager().restartLoader(0, null, this);
    }
    
    private void onMenuHelp() {
    }
    
    @Override
    public Loader<TaskContext> onCreateLoader(int id, Bundle args) {
        final TaskContext taskContext = taskContexts[id];
        taskContext.state = TaskState.RUNNING;
        taskContextAdapter.notifyDataSetChanged();
        
        return new TaskExecutor(getActivity(), taskContext);
    }
    
    @Override
    public void onLoaderReset(Loader<TaskContext> loader) {
    }
    
    @Override
    public void onLoadFinished(Loader<TaskContext> loader, TaskContext result) {
        taskContextAdapter.notifyDataSetChanged();
        
        final int nextId = loader.getId() + 1;
        if (nextId != taskContexts.length) {
            getLoaderManager().restartLoader(nextId, null, this);
        } else {
            startMenuItem.setVisible(true);
        }
    }
    
    /**
     * Execute {@link Task} instance in a background thread.
     * @author Pixmob
     */
    private static class TaskExecutor extends AsyncTaskLoader<TaskContext> {
        private final TaskContext taskContext;
        
        public TaskExecutor(final FragmentActivity context,
                final TaskContext taskContext) {
            super(context);
            this.taskContext = taskContext;
        }
        
        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
        }
        
        @Override
        public TaskContext loadInBackground() {
            final String taskName = taskContext.task.getName();
            Log.i(TAG, "Executing task: " + taskName);
            try {
                taskContext.task.run();
                taskContext.state = TaskState.FINISHED;
                Log.i(TAG, "Task successfully executed: " + taskName);
            } catch (TaskExecutionFailedException e) {
                taskContext.state = TaskState.FAILED;
                Log.e(TAG, "Task execution failed: " + taskName, e);
            }
            
            return taskContext;
        }
    }
    
    /**
     * {@link ArrayAdapter} implementation for displaying {@link Task}
     * instances.
     * @author Pixmob
     */
    private static class TaskContextAdapter extends ArrayAdapter<TaskContext> {
        private static final Map<TaskState, Integer> TASK_COLORS = new HashMap<TaskListFragment.TaskState, Integer>(
                4);
        private final LayoutInflater layoutInflater;
        
        public TaskContextAdapter(final Context context,
                final TaskContext[] taskContexts) {
            super(context, R.layout.task_row, taskContexts);
            layoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
            // Lazy initialize colors.
            if (TASK_COLORS.isEmpty()) {
                final Resources r = context.getResources();
                TASK_COLORS.put(TaskState.RUNNABLE,
                    r.getColor(R.color.task_color_runnable));
                TASK_COLORS.put(TaskState.RUNNING,
                    r.getColor(R.color.task_color_running));
                TASK_COLORS.put(TaskState.FINISHED,
                    r.getColor(R.color.task_color_finished));
                TASK_COLORS.put(TaskState.FAILED,
                    r.getColor(R.color.task_color_failed));
            }
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View row;
            if (convertView == null) {
                row = layoutInflater.inflate(R.layout.task_row, parent, false);
            } else {
                row = convertView;
            }
            
            final TaskContext taskContext = getItem(position);
            final TextView tv = (TextView) row.findViewById(R.id.task_name);
            tv.setText(taskContext.task.getName());
            tv.setTextColor(TASK_COLORS.get(taskContext.state));
            
            final ProgressBar taskProgress = (ProgressBar) row
                    .findViewById(R.id.task_progress);
            if (TaskState.RUNNING.equals(taskContext.state)) {
                taskProgress.setVisibility(View.VISIBLE);
            } else {
                taskProgress.setVisibility(View.GONE);
            }
            
            return row;
        }
    }
    
    /**
     * {@link Task} context.
     * @author Pixmob
     */
    public static class TaskContext {
        public final Task task;
        public TaskState state = TaskState.RUNNABLE;
        
        public TaskContext(final Task task) {
            this.task = task;
        }
    }
    
    /**
     * {@link Task} state.
     * @author Pixmob
     */
    private static enum TaskState {
        RUNNABLE, RUNNING, FINISHED, FAILED
    }
}
