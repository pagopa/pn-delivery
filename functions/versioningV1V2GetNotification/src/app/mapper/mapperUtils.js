exports.adjustTimelineAndHistory = function(timeline, history, categoriesToExclude, transformFunction) {
    const adjustedTimeline = adjustTimeline(timeline, categoriesToExclude, transformFunction);
    
    const timelineIds = [];
    for (const tl of adjustedTimeline) timelineIds.push(tl.elementId);
    const adjustedHistory = adjustHistory(history, timelineIds);
    
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
function adjustHistory(history, timelineIds) {
    const notificationStatusHistory =
    history.filter((nsh) => {
        let keep = true;
        for (const relatedTimelineElement of nsh.relatedTimelineElements) {
            keep = timelineIds.includes(relatedTimelineElement);
            if (!keep) {
                console.log("NotificationStatusHistory - skipping status:", nsh.status, "caused by relatedTimelineElement:", relatedTimelineElement);
                return keep;
            }
        }
        return keep;
    });
    return notificationStatusHistory;
}
