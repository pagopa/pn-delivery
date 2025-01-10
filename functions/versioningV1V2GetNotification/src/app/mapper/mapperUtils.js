exports.adjustTimelineAndHistory = function(timeline, history, categoriesToExclude, transformFunction, transformHistory = undefined) {
    const adjustedTimeline = adjustTimeline(timeline, categoriesToExclude, transformFunction);
    
    const timelineIds = [];
    for (const tl of adjustedTimeline) timelineIds.push(tl.elementId);
    const adjustedHistory = adjustHistory(history, timelineIds, transformHistory);
    
    return adjustedTimelineAndHistory = {
        timeline: adjustedTimeline,
        history: adjustedHistory
    }
}

function adjustTimeline(timeline, categoriesToExclude, transformFunction) {
    const timelineRaw = timeline.filter((tl) =>
        !categoriesToExclude.includes(tl.category)
    );
    
    if(transformFunction) {
        const timeline = timelineRaw.map((tl) => transformFunction(tl));
        return timeline;
    } else {
        return timelineRaw;
    }
}

// elimina dalla status history tutti gli elementi che includono come related timeline
function adjustHistory(history, timelineIds, transformHistory) {
    const adjustedHistory = history.filter((nsh) => {
        let keep = false;
        let copyOfRelatedElements = [...nsh.relatedTimelineElements];
        //If at least one timelineElement is in timelineIds then keep will be true and i will not remove the historyElement, otherwise it will be removed.
        for (const relatedTimelineElement of copyOfRelatedElements) {
            if (timelineIds.includes(relatedTimelineElement)) {
                keep = true
            } else {
                nsh.relatedTimelineElements.splice(nsh.relatedTimelineElements.indexOf(relatedTimelineElement), 1);
            }
        }
        if (nsh.relatedTimelineElements.length === 0) {
            console.log("NotificationStatusHistory - skipping status: ", nsh.status, " because no timeline elements are left in it.");
            return false;
        }
        return keep;
    });

    if(transformHistory) {
        return transformHistory(timelineIds, adjustedHistory);
    }
    return adjustedHistory;
}