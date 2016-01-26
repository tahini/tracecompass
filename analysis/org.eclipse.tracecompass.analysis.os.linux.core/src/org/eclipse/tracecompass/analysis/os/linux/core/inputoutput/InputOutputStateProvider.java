package org.eclipse.tracecompass.analysis.os.linux.core.inputoutput;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author Houssem Daoud
 * @since 2.0
 */
public class InputOutputStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 1;
    private final IKernelAnalysisEventLayout fLayout;

    String[] readArray = { LttngStrings.SYSTEM_READ, LttngStrings.SYSTEM_READV, LttngStrings.SYSTEM_PREAD, LttngStrings.SYSTEM_PREADV };
    List<String> readList = Arrays.asList(readArray);

    String[] writeArray = { LttngStrings.SYSTEM_WRITE, LttngStrings.SYSTEM_WRITEV, LttngStrings.SYSTEM_PWRITE, LttngStrings.SYSTEM_PWRITEV };
    List<String> writeList = Arrays.asList(writeArray);

    class Disk {
        public Integer dev;
        public String diskname;
        // Map<Integer, Request> preparedRequests = new HashMap<>();
        Map<Integer, Request> driverqueue;
        Map<Integer, Request> waitingqueue;

        public Disk(Integer dev, String diskname) {
            super();
            this.diskname = diskname;
            this.dev = dev;
            driverqueue = new HashMap<>();
            waitingqueue = new HashMap<>();
        }
    }

    class Bio {
        public Integer sector;
        public Integer nr_sector;
        public Disk disk;
        public int rwbs;

        public Bio(Integer sector, Integer nr_sector, Disk disk, int rwbs) {
            super();
            this.sector = sector;
            this.nr_sector = nr_sector;
            this.disk = disk;
            this.rwbs = rwbs;
        }
    }

    class Request {
        public Integer sector;
        public Integer nr_sector;
        public Disk disk;
        public Vector<Bio> bios;
        Integer slotQuark;
        public int rwbs;

        public Request(Integer sector) {
            super();
            this.sector = sector;
            bios = new Vector<>();
            this.slotQuark = -1;
        }

         public Request(Bio bio) {
         super();
         this.sector = bio.sector;
         this.nr_sector = bio.nr_sector;
         this.rwbs = bio.rwbs;
         bios = new Vector<>();
         bios.add(0, bio);
         this.slotQuark = -1;
         }
    }

    Map<Integer, Disk> disks = new HashMap<>();

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     * @param layout
     *            The event layout to use for this state provider.
     */
    public InputOutputStateProvider(@NonNull ITmfTrace trace, IKernelAnalysisEventLayout layout) {
        super(trace, "Input Output Analysis");//$NON-NLS-1$
        fLayout = layout;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public InputOutputStateProvider getNewInstance() {
        return new InputOutputStateProvider(this.getTrace(), this.fLayout);
    }

    @Override
    protected void eventHandle(ITmfEvent event) {

        final ITmfEventField content = event.getContent();
        final String eventName = event.getType().getName();
        final long ts = event.getTimestamp().getValue();

        try {
            final ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
            switch (eventName) {

            case LttngStrings.ELV_MERGE_REQUESTS: {
                Integer phydisk = ((Long) content.getField(LttngStrings.DEV).getValue()).intValue();
                Integer baseRequest_sector = ((Long) content.getField(LttngStrings.RQ_SECTOR).getValue()).intValue();
                Integer mergedRequest_sector = ((Long) content.getField(LttngStrings.NEXTRQ_SECTOR).getValue()).intValue();
                Disk disk = disks.get(phydisk);
                if (disk == null) {
                    return;
                }
                Request baseRequest = disk.waitingqueue.get(baseRequest_sector);
                if (baseRequest == null) {
                    return;
                }
                Request mergedRequest = disk.waitingqueue.get(mergedRequest_sector);
                if (mergedRequest == null) {
                    return;
                }
                disk.waitingqueue.remove(baseRequest.sector);
                disk.waitingqueue.remove(mergedRequest.sector);
                Request final_request = merge_request(baseRequest, mergedRequest);
                disk.waitingqueue.put(final_request.sector, final_request);
                remove_from_queue(ss, ts, mergedRequest, final_request.slotQuark);
                update_queues_length(ss, ts, disk);
            }
                break;

            case LttngStrings.BLOCK_BIO_FRONTMERGE: {
                Integer sector = ((Long) content.getField(LttngStrings.SECTOR).getValue()).intValue();
                Integer rq_sector = ((Long) content.getField(LttngStrings.RQ_SECTOR).getValue()).intValue();// $NON-NLS-1$
                Integer nr_sector = ((Long) content.getField(LttngStrings.NR_SECTOR).getValue()).intValue();
                Integer phydisk = ((Long) content.getField(LttngStrings.DEV).getValue()).intValue();
                Integer rwbs = ((Long) content.getField(LttngStrings.RWBS).getValue()).intValue();
                Disk disk = disks.get(phydisk);
                if (disk == null) {
                    return;
                }
                Request request = disk.waitingqueue.get(rq_sector);
                if (request == null) {
                    return;
                }
                disk.waitingqueue.remove(request.sector);
                Bio bio = new Bio(sector, nr_sector, disk, rwbs % 2);
                insert_bio(request, bio);
                disk.waitingqueue.put(request.sector, request);
                remove_from_queue(ss, ts, request, null);
                insert_in_queue(ss, ts, request, disk.diskname, Attributes.WAITING_QUEUE, null);
            }
                break;

            case LttngStrings.BLOCK_RQ_INSERT: {
                Integer phydisk = ((Long) content.getField(LttngStrings.DEV).getValue()).intValue();
                Integer sector = ((Long) content.getField(LttngStrings.SECTOR).getValue()).intValue();
                Integer nr_sector = ((Long) content.getField(LttngStrings.NR_SECTOR).getValue()).intValue();
                Integer rwbs = ((Long) content.getField(LttngStrings.RWBS).getValue()).intValue();
                Disk disk = disks.get(phydisk);
                if (disk == null) {
                    return;
                }
                if (nr_sector.intValue() == 0) {
                    return;
                }
                Bio bio = new Bio(sector, nr_sector, disk, rwbs % 2);
                Request request = new Request(bio);
                disk.waitingqueue.put(request.sector, request);
                insert_in_queue(ss, ts, request, disk.diskname, Attributes.WAITING_QUEUE, null);
                update_queues_length(ss, ts, disk);
            }
                break;

            case LttngStrings.BLOCK_RQ_ISSUE: {
                Integer phydisk = ((Long) content.getField(LttngStrings.DEV).getValue()).intValue();
                Integer sector = ((Long) content.getField(LttngStrings.SECTOR).getValue()).intValue();
                Integer nr_sector = ((Long) content.getField(LttngStrings.NR_SECTOR).getValue()).intValue();
                Disk disk = disks.get(phydisk);
                if (disk == null) {
                    return;
                }
                if (nr_sector.intValue() == 0) {
                    return;
                }
                Request request = disk.waitingqueue.get(sector);
                if (request == null) {
                    return;
                }
                request.nr_sector = nr_sector;
                disk.waitingqueue.remove(request.sector);
                Integer previousSlot = request.slotQuark;
                remove_from_queue(ss, ts, request, null);
                disk.driverqueue.put(request.sector, request);
                insert_in_queue(ss, ts, request, disk.diskname, Attributes.DRIVER_QUEUE, previousSlot);
                update_queues_length(ss, ts, disk);
            }
                break;

            case LttngStrings.BLOCK_RQ_COMPLETE: {
                Integer sector = ((Long) content.getField(LttngStrings.SECTOR).getValue()).intValue();
                Integer nr_sector = ((Long) content.getField(LttngStrings.NR_SECTOR).getValue()).intValue();
                Integer phydisk = ((Long) content.getField(LttngStrings.DEV).getValue()).intValue();
                Integer rwbs = ((Long) content.getField(LttngStrings.RWBS).getValue()).intValue();
                Disk disk = disks.get(phydisk);
                if (disk == null) {
                    return;
                }
                Integer diskquark = ss.getQuarkRelativeAndAdd(getNodeDisks(ss), disk.diskname);
                if (rwbs % 2 == 0) {
                    Integer readQuark = ss.getQuarkRelativeAndAdd(diskquark, Attributes.SECTORS_READ);
                    ss.modifyAttribute(ts, TmfStateValue.newValueInt(increment(ss, readQuark, nr_sector)), readQuark);
                } else {
                    Integer writtenQuark = ss.getQuarkRelativeAndAdd(diskquark, Attributes.SECTORS_WRITTEN);
                    ss.modifyAttribute(ts, TmfStateValue.newValueInt(increment(ss, writtenQuark, nr_sector)), writtenQuark);
                }
                Request request = disk.driverqueue.get(sector);
                if (request == null) {
                    return;
                }
                disk.driverqueue.remove(sector);
                remove_from_queue(ss, ts, request, null);
                update_queues_length(ss, ts, disk);
            }
                break;

            case LttngStrings.LTTNG_STATEDUMP_BLOCK_DEVICE: {
                String diskname = (String) event.getContent().getField(LttngStrings.DISKNAME).getValue();
                Integer dev = ((Long) content.getField(LttngStrings.DEV).getValue()).intValue();
                Disk disk = new Disk(dev, diskname);
                disks.put(dev, disk);
            }
                break;

            default: {
                if (eventName.startsWith(LttngStrings.SYSCALL_PREFIX)) {
                    Integer tid = ((Long) content.getField(LttngStrings.CONTEXT_TID).getValue()).intValue();
                    Integer sys_ThreadNode = ss.getQuarkRelativeAndAdd(getNodeSyscalls(ss), String.valueOf(tid));
                    Integer quark = ss.getQuarkRelativeAndAdd(sys_ThreadNode, Attributes.SYSTEM_CALL);
                    TmfStateValue value = TmfStateValue.newValueString(eventName);
                    ss.modifyAttribute(ts, value, quark);
                }
                if (eventName.startsWith(LttngStrings.EXIT_SYSCALL)) {
                    Integer tid = ((Long) content.getField(LttngStrings.CONTEXT_TID).getValue()).intValue();
                    Integer pid = ((Long) content.getField(LttngStrings.CONTEXT_PID).getValue()).intValue();
                    Integer ret = ((Long) event.getContent().getField(LttngStrings.RETURN).getValue()).intValue();
                    Integer sys_ThreadNode = ss.getQuarkRelative(getNodeSyscalls(ss), String.valueOf(tid));
                    Integer syscallQuark = ss.getQuarkRelative(sys_ThreadNode, Attributes.SYSTEM_CALL);
                    ITmfStateValue currentSyscall = ss.queryOngoingState(syscallQuark);
                    String syscallValue = currentSyscall.unboxStr();
                    if (ret.intValue() >= 0) {
                        if (readList.contains(syscallValue)) {
                            Integer currentProcessNode = ss.getQuarkRelativeAndAdd(getNodeThreads(ss), String.valueOf(pid));
                            Integer readQuark = ss.getQuarkRelativeAndAdd(currentProcessNode, Attributes.BYTES_READ);
                            ss.getQuarkRelativeAndAdd(currentProcessNode, Attributes.BYTES_WRITTEN);
                            TmfStateValue readValue = TmfStateValue.newValueInt(increment(ss, readQuark, ret));
                            ss.modifyAttribute(ts, readValue, readQuark);
                        } else if (writeList.contains(syscallValue)) {
                            Integer currentProcessNode = ss.getQuarkRelativeAndAdd(getNodeThreads(ss), String.valueOf(pid));
                            ss.getQuarkRelativeAndAdd(currentProcessNode, Attributes.BYTES_READ);
                            Integer writtenQuark = ss.getQuarkRelativeAndAdd(currentProcessNode, Attributes.BYTES_WRITTEN);
                            TmfStateValue writtenValue = TmfStateValue.newValueInt(increment(ss, writtenQuark, ret));
                            ss.modifyAttribute(ts, writtenValue, writtenQuark);
                        }
                    }
                    TmfStateValue value = TmfStateValue.nullValue();
                    ss.modifyAttribute(ts, value, syscallQuark);
                }
            }
                break;
            }

        } catch (AttributeNotFoundException ae) {
            ae.printStackTrace();
        } catch (TimeRangeException tre) {
            tre.printStackTrace();
        } catch (StateValueTypeException sve) {
            sve.printStackTrace();
        }
    }

    private static int getNodeDisks(ITmfStateSystemBuilder ssb) {
        return ssb.getQuarkAbsoluteAndAdd(Attributes.DISKS);
    }

    private static Integer increment(ITmfStateSystemBuilder ssb, Integer quark, Integer size) {
        try {
            ITmfStateValue value = ssb.queryOngoingState(quark);
            if (value.isNull()) {
                value = TmfStateValue.newValueInt(0);
            }
            Integer longValue = value.unboxInt();
            longValue = longValue + size;
            return longValue;
        } catch (AttributeNotFoundException | TimeRangeException | StateValueTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    static private void insert_bio(Request req, Bio bio) {
        req.bios.add(bio);
        req.nr_sector += bio.nr_sector;
        if (bio.sector < req.sector) {
            req.sector = bio.sector;
        }
    }

    private Request merge_request(Request first_req, Request second_req) {
        Request req = new Request(first_req.sector);
        req.disk = first_req.disk;
        req.nr_sector = first_req.nr_sector + second_req.nr_sector;
        req.slotQuark = first_req.slotQuark;
        req.bios.addAll(first_req.bios);
        req.bios.addAll(second_req.bios);
        req.rwbs = first_req.rwbs;
        return req;
    }

    private static Integer getFirstAvailableSlot(ITmfStateSystemBuilder ssb, long ts, String diskname, String queue_name) {
        Integer diskquark = ssb.getQuarkRelativeAndAdd(getNodeDisks(ssb), diskname);
        Integer waitingQueueQuark = ssb.getQuarkRelativeAndAdd(diskquark, queue_name);
        try {
            List<Integer> slotsQuarks = ssb.getSubAttributes(waitingQueueQuark, false);
            for (Integer slotQuark : slotsQuarks) {
                Integer slotStatusQuark = ssb.getQuarkRelative(slotQuark, Attributes.STATUS);
                ITmfStateValue value = ssb.querySingleState(ts, slotStatusQuark).getStateValue();
                if (value.isNull()) {
                    return slotQuark;
                }
            }
            Integer i = slotsQuarks.size() + 1;
            String number = String.valueOf(i);
            Integer slotQuark = ssb.getQuarkRelativeAndAdd(waitingQueueQuark, number);
            ssb.getQuarkRelativeAndAdd(slotQuark, Attributes.STATUS);
            return slotQuark;
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
            return -1;
        } catch (StateSystemDisposedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static void insert_in_queue(ITmfStateSystemBuilder ssb, long ts, Request request, String diskname, String queue_name, Integer previousSlot) throws AttributeNotFoundException {

        ITmfStateValue statusState = request.rwbs == 0 ? StateValues.READING_REQUEST_VALUE : StateValues.WRITING_REQUEST_VALUE;
        /* Insertion in waiting queue */
        Integer slotQuark = getFirstAvailableSlot(ssb, ts, diskname, queue_name);
        if (slotQuark.intValue() == -1) {
            System.out.println("Error in finding Slot"); //$NON-NLS-1$
            System.exit(-1);
        }
        Integer statusQuark = ssb.getQuarkRelativeAndAdd(slotQuark, Attributes.STATUS);
        ssb.modifyAttribute(ts, statusState, statusQuark);
        Integer currentRequestQuark = ssb.getQuarkRelativeAndAdd(slotQuark, Attributes.CURRENT_REQUEST);
        ssb.modifyAttribute(ts, TmfStateValue.newValueLong(request.sector), currentRequestQuark);
        Integer requestInSlotSizeQuark = ssb.getQuarkRelativeAndAdd(slotQuark, Attributes.REQUEST_SIZE);
        ssb.modifyAttribute(ts, TmfStateValue.newValueLong(request.nr_sector), requestInSlotSizeQuark);
        if (previousSlot != null) {
            Integer IssuedFromQuark = ssb.getQuarkRelativeAndAdd(slotQuark,Attributes.ISSUED_FROM);
            ssb.modifyAttribute(ts,TmfStateValue.newValueLong(previousSlot), IssuedFromQuark);
        }
        Integer mergedInQuark = ssb.getQuarkRelativeAndAdd(slotQuark,Attributes.MERGED_IN);
        ssb.modifyAttribute(ts,TmfStateValue.nullValue(), mergedInQuark);
        request.slotQuark = slotQuark;
    }

    private static void remove_from_queue(ITmfStateSystemBuilder ssb, long ts, Request request, Integer nextSlot) throws StateValueTypeException, AttributeNotFoundException {

        // set the queue slot as empty
        Integer queueStatusQuark = ssb.getQuarkRelativeAndAdd(request.slotQuark, Attributes.STATUS);
        ssb.modifyAttribute(ts, TmfStateValue.nullValue(), queueStatusQuark);
        Integer currentRequestQuark = ssb.getQuarkRelativeAndAdd(request.slotQuark, Attributes.CURRENT_REQUEST);
        ssb.modifyAttribute(ts, TmfStateValue.nullValue(), currentRequestQuark);
        Integer requestSizeQuark = ssb.getQuarkRelativeAndAdd(request.slotQuark, Attributes.REQUEST_SIZE);
        ssb.modifyAttribute(ts, TmfStateValue.nullValue(), requestSizeQuark);
        Integer MergedInQuark = ssb.getQuarkRelativeAndAdd(request.slotQuark,Attributes.MERGED_IN);
        ssb.modifyAttribute(ts,TmfStateValue.nullValue(), MergedInQuark);
        Integer IssuedFromQuark = ssb.getQuarkRelativeAndAdd(request.slotQuark,Attributes.ISSUED_FROM);
        ssb.modifyAttribute(ts,TmfStateValue.nullValue(), IssuedFromQuark);
        if (nextSlot != null) {
          MergedInQuark = ssb.getQuarkRelativeAndAdd(request.slotQuark,Attributes.MERGED_IN);
          ssb.modifyAttribute(ts,TmfStateValue.newValueLong(nextSlot), MergedInQuark);
        }
        request.slotQuark = null;
    }

    private static void update_queues_length(ITmfStateSystemBuilder ssb, long ts, Disk disk) throws StateValueTypeException, AttributeNotFoundException {
        Integer diskquark = ssb.getQuarkRelativeAndAdd(getNodeDisks(ssb), disk.diskname);
        Integer driverqueue_length_quark = ssb.getQuarkRelativeAndAdd(diskquark, Attributes.DRIVERQUEUE_LENGTH);
        ssb.modifyAttribute(ts, TmfStateValue.newValueLong(disk.driverqueue.size()), driverqueue_length_quark);
        Integer waitingqueue_length_quark = ssb.getQuarkRelativeAndAdd(diskquark, Attributes.WAITINGQUEUE_LENGTH);
        ssb.modifyAttribute(ts, TmfStateValue.newValueLong(disk.waitingqueue.size()), waitingqueue_length_quark);
    }

    private static int getNodeThreads(ITmfStateSystemBuilder ssb) {
        return ssb.getQuarkAbsoluteAndAdd(Attributes.THREADS);
    }

    private static int getNodeSyscalls(ITmfStateSystemBuilder ssb) {
        return ssb.getQuarkAbsoluteAndAdd(Attributes.SYSTEM_CALLS_ROOT);
    }

}
