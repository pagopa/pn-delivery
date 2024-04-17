exports.adjustTimelineAndHistory = function(timeline, history, categoriesToExclude, transformFunction) {
    const adjustedTimeline = adjustTimeline(timeline, categoriesToExclude, transformFunction);
    
    const timelineIds = [];
    for (const tl of adjustedTimeline) {
        console.log('aggiungiamo '+tl.elementId);
        timelineIds.push(tl.elementId);
    }
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
    const filteredElements = [];

    for (const nsh of history){
        filteredElements.push (adjustHistoryElement(nsh, timelineIds));
    }

    return filteredElements;
}

function adjustHistoryElement(nsh, timelineIds){
    const filteredElements = [];
    for (const relatedTimelineElement of nsh.relatedTimelineElements) {
        if (timelineIds.includes(relatedTimelineElement)) {
            filteredElements.push(relatedTimelineElement);
        }
    }
    return historyElement = {
        status: nsh.status,
        activeFrom: nsh.activeFrom,
        relatedTimelineElements: filteredElements
    };
}