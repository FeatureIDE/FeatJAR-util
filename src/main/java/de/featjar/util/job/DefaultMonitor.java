/*
 * Copyright (C) 2022 Sebastian Krieter, Elias Kuiter
 *
 * This file is part of util.
 *
 * util is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * util is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with util. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatJAR/util> for further information.
 */
package de.featjar.util.job;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Default implementation of {@link InternalMonitor} and {@link Monitor}.<br>
 * Provides support for reporting progress and canceling a function's execution.
 *
 * @author Sebastian Krieter
 */
public class DefaultMonitor implements InternalMonitor {

    protected final List<DefaultMonitor> children = new CopyOnWriteArrayList<>();
    protected final DefaultMonitor parent;
    protected final int parentWork;

    protected String taskName;
    protected Supplier<String> statusReporter;

    protected boolean canceled, done;
    protected long currentWork;
    protected long totalWork;

    public DefaultMonitor() {
        parent = null;
        parentWork = 0;
    }

    private DefaultMonitor(DefaultMonitor parent, int parentWork) {
        this.parent = parent;
        canceled = parent.canceled;
        done = parent.done;
        this.parentWork = parentWork;
    }

    protected void uncertainWorked(long work) {
        synchronized (this) {
            currentWork += work;
            totalWork += work;
        }
    }

    protected void worked(long work) {
        synchronized (this) {
            currentWork += work;
        }
    }

    @Override
    public final void uncertainStep() throws MethodCancelException {
        uncertainWorked(1);
        checkCancel();
    }

    @Override
    public final void uncertainStep(long work) throws MethodCancelException {
        uncertainWorked(work);
        checkCancel();
    }

    @Override
    public final void step() throws MethodCancelException {
        worked(1);
        checkCancel();
    }

    @Override
    public final void step(long work) throws MethodCancelException {
        worked(work);
        checkCancel();
    }

    @Override
    public final void setTotalWork(long work) {
        totalWork = work;
        checkCancel();
    }

    @Override
    public void done() {
        currentWork = totalWork;
        done = true;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public void checkCancel() throws MethodCancelException {
        if (canceled) {
            throw new MethodCancelException();
        }
        if (parent != null) {
            parent.checkCancel();
        }
    }

    @Override
    public long getTotalWork() {
        return totalWork;
    }

    @Override
    public long getRemainingWork() {
        return totalWork - getWorkDone();
    }

    @Override
    public long getWorkDone() {
        long workDone = currentWork;
        for (final DefaultMonitor child : children) {
            workDone += child.getRelativeWorkDone() * child.parentWork;
        }
        return workDone;
    }

    @Override
    public double getRelativeWorkDone() {
        if (totalWork == 0) {
            return 0;
        }
        double workDone = currentWork;
        for (final DefaultMonitor child : children) {
            workDone += child.getRelativeWorkDone() * child.parentWork;
        }
        return workDone / totalWork;
    }

    @Override
    public void setTaskName(String name) {
        taskName = name;
    }

    @Override
    public String getTaskName() {
        return String.valueOf(taskName);
    }

    @Override
    public DefaultMonitor subTask(int size) {
        final DefaultMonitor child = new DefaultMonitor(this, size);
        children.add(child);
        return child;
    }

    @Override
    public void setStatusReporter(Supplier<String> reporter) {
        statusReporter = reporter;
    }

    @Override
    public String reportStatus() {
        return statusReporter == null ? "" : statusReporter.get();
    }
}
