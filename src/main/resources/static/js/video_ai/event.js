document.addEventListener('DOMContentLoaded', () => {
    'use strict';

    // 화면 데모용 회의 목록은 그대로 두고, 상단 전역 챗봇만 실제 API에 연결한다.
    const meetings = [
        {
            id: 'rec-001',
            title: '프로젝트 킥오프 회의',
            date: '2024-03-15',
            handle: '@kim_pm',
            audioUrl: '',
            summaryTitle: '프로젝트 킥오프 회의 요약',
            summaryText: '1. 프로젝트 일정 확인\n- 3월 말까지 MVP 완성 목표\n- QA 기간 1주일 추가 필요\n\n2. 디자인 리뷰\n- 메인 페이지 UI 수정 사항 논의\n- 모바일 반응형 대응 필요\n\n3. 다음 회의\n- 3월 20일 오전 10시'
        },
        {
            id: 'rec-002',
            title: 'Spring Security 강의',
            date: '2024-03-10',
            handle: '@lee_dev',
            audioUrl: '',
            summaryTitle: 'Spring Security 강의 요약',
            summaryText: 'Spring Boot 보안 설정 강의\n\n1. JWT 토큰 기반 인증\n2. Spring Security 필터 체인\n3. CORS 설정 방법\n4. CSRF 보호 전략'
        },
        {
            id: 'rec-003',
            title: '기술 면접 인터뷰',
            date: '2024-03-08',
            handle: '@park_hr',
            audioUrl: '',
            summaryTitle: '기술 면접 인터뷰 요약',
            summaryText: '후보자 기술 면접 내용\n\n- Java/Spring 경력 3년\n- MSA 프로젝트 경험\n- 커뮤니케이션 우수\n\n결론: 2차 면접 진행 권장'
        }
    ];

    let currentMeeting = null;
    let expandedMeetingId = null;
    let assistantRequestInFlight = false;

    const fab = document.getElementById('vaiFab');
    const dropdown = document.getElementById('vaiDropdown');
    const assistantToggle = document.getElementById('vaiAssistantToggle');
    const meetingToggle = document.getElementById('vaiMeetingToggle');
    const meetingChevron = document.getElementById('vaiMeetingChevron');
    const meetingList = document.getElementById('vaiMeetingList');

    const assistantPanel = document.getElementById('vaiAssistantPanel');
    const assistantMessages = document.getElementById('vaiAssistantMessages');
    const assistantTextarea = document.getElementById('vaiAssistantTextarea');
    const assistantSendBtn = document.getElementById('vaiAssistantSend');
    const assistantBack = document.getElementById('vaiAssistantBack');
    const assistantClose = document.getElementById('vaiAssistantClose');

    const audioPanel = document.getElementById('vaiAudioPanel');
    const audioPlayer = document.getElementById('vaiAudioPlayer');
    const audioTitle = document.getElementById('vaiAudioTitle');
    const audioBack = document.getElementById('vaiAudioBack');
    const audioClose = document.getElementById('vaiAudioClose');

    const summaryPanel = document.getElementById('vaiSummaryPanel');
    const summaryTitle = document.getElementById('vaiSummaryTitle');
    const summaryText = document.getElementById('vaiSummaryText');
    const summaryBack = document.getElementById('vaiSummaryBack');
    const summaryClose = document.getElementById('vaiSummaryClose');

    const meetingChatPanel = document.getElementById('vaiChatPanel');
    const meetingChatTitle = document.getElementById('vaiChatTitle');
    const meetingChatMessages = document.getElementById('vaiChatMessages');
    const meetingChatTextarea = document.querySelector('.vai-chat-textarea:not(#vaiAssistantTextarea)');
    const meetingChatSendBtn = document.getElementById('vaiChatSend');
    const meetingChatBack = document.getElementById('vaiChatBack');
    const meetingChatClose = document.getElementById('vaiChatClose');

    function renderMeetings() {
        let html = '';

        for (let i = 0; i < meetings.length; i++) {
            const meeting = meetings[i];

            html +=
                '<div class="vai-meeting-item" data-id="' + meeting.id + '">' +
                '  <div class="vai-meeting-row">' +
                '    <div class="vai-meeting-info">' +
                '      <span class="vai-meeting-title">' + meeting.title + '</span>' +
                '      <div class="vai-meeting-meta">' +
                '        <span>' + meeting.date + '</span>' +
                '        <span class="vai-meeting-dot">&middot;</span>' +
                '        <span class="vai-meeting-handle">' + meeting.handle + '</span>' +
                '      </div>' +
                '    </div>' +
                '    <div class="vai-meeting-chevron">' +
                '      <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">' +
                '        <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"></path>' +
                '      </svg>' +
                '    </div>' +
                '  </div>' +
                '  <div class="vai-sub-actions">' +
                '    <div class="vai-sub-action" data-action="audio">' +
                '      <div class="vai-sub-action-icon">' +
                '        <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M8 5v14l11-7z"></path></svg>' +
                '      </div>' +
                '      <span class="vai-sub-action-label">오디오 재생</span>' +
                '    </div>' +
                '    <div class="vai-sub-action" data-action="summary">' +
                '      <div class="vai-sub-action-icon">' +
                '        <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zM6 20V4h7v5h5v11H6zm2-6h8v2H8v-2zm0-4h8v2H8v-2zm0 8h5v2H8v-2z"></path></svg>' +
                '      </div>' +
                '      <span class="vai-sub-action-label">요약본 보기</span>' +
                '    </div>' +
                '    <div class="vai-sub-action" data-action="chat">' +
                '      <div class="vai-sub-action-icon">' +
                '        <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M20.93 11.94c0-4.6-3.95-8.42-8.93-8.42s-8.93 3.82-8.93 8.42c-.01.83.13 1.6.33 2.29.1.34.21.65.3.94.1.3.18.55.25.79.13.48.19.94.11 1.46-.08.6-.27 1.23-.58 1.91 1.3.39 2.62.06 4.12-.61l.47-.21.44.25c1.1.61 1.87 1.11 3.49 1.11 4.98 0 8.93-3.63 8.93-8.18z"></path></svg>' +
                '      </div>' +
                '      <span class="vai-sub-action-label">이 회의에 대해 질문</span>' +
                '    </div>' +
                '  </div>' +
                '</div>';
        }

        meetingList.innerHTML = html;
        bindMeetingEvents();
    }

    function findMeeting(id) {
        for (let i = 0; i < meetings.length; i++) {
            if (meetings[i].id === id) {
                return meetings[i];
            }
        }
        return null;
    }

    function bindMeetingEvents() {
        const rows = meetingList.querySelectorAll('.vai-meeting-row');
        rows.forEach(function (row) {
            row.addEventListener('click', function (event) {
                event.stopPropagation();
                const item = row.closest('.vai-meeting-item');
                const id = item.getAttribute('data-id');
                const subActions = item.querySelector('.vai-sub-actions');
                const chevron = item.querySelector('.vai-meeting-chevron');

                if (expandedMeetingId === id) {
                    subActions.classList.remove('open');
                    chevron.classList.remove('expanded');
                    expandedMeetingId = null;
                    currentMeeting = null;
                    return;
                }

                collapseAllSubActions(true);
                subActions.classList.add('open');
                chevron.classList.add('expanded');
                expandedMeetingId = id;
                currentMeeting = findMeeting(id);
            });
        });

        const subActions = meetingList.querySelectorAll('.vai-sub-action');
        subActions.forEach(function (button) {
            button.addEventListener('click', function (event) {
                event.stopPropagation();
                const action = button.getAttribute('data-action');
                if (!currentMeeting) {
                    return;
                }

                if (action === 'audio') {
                    openAudio();
                    return;
                }

                if (action === 'summary') {
                    openSummary();
                    return;
                }

                if (action === 'chat') {
                    openMeetingChat();
                }
            });
        });
    }

    function collapseAllSubActions(instant) {
        const allSubs = meetingList.querySelectorAll('.vai-sub-actions');
        const allChevrons = meetingList.querySelectorAll('.vai-meeting-chevron');

        allSubs.forEach(function (subAction) {
            if (instant) {
                subAction.classList.add('no-transition');
            }
            subAction.classList.remove('open');
            if (instant) {
                subAction.offsetHeight;
                subAction.classList.remove('no-transition');
            }
        });

        allChevrons.forEach(function (chevron) {
            chevron.classList.remove('expanded');
        });

        expandedMeetingId = null;
    }

    function closeAllPanels() {
        assistantPanel.classList.remove('open');
        audioPanel.classList.remove('open');
        summaryPanel.classList.remove('open');
        meetingChatPanel.classList.remove('open');
        audioPlayer.pause();
    }

    function closeEverything() {
        dropdown.classList.remove('open');
        meetingList.classList.remove('open');
        meetingChevron.classList.remove('expanded');
        collapseAllSubActions();
        closeAllPanels();
    }

    function backToDropdown() {
        closeAllPanels();
        dropdown.classList.add('open');
    }

    function removeEmptyState(target) {
        const empty = target.querySelector('.vai-chat-empty');
        if (empty) {
            empty.remove();
        }
    }

    function addMessage(target, text, type, extraClass) {
        removeEmptyState(target);

        const bubble = document.createElement('div');
        bubble.className = 'vai-msg ' + (type === 'user' ? 'vai-msg-user' : 'vai-msg-ai');

        if (extraClass) {
            bubble.classList.add(extraClass);
        }

        bubble.textContent = text;
        target.appendChild(bubble);
        target.scrollTop = target.scrollHeight;
        return bubble;
    }

    function setAssistantComposerDisabled(disabled) {
        assistantTextarea.disabled = disabled;
        assistantSendBtn.disabled = disabled;
    }

    function openAudio() {
        audioTitle.textContent = currentMeeting.title + ' - 오디오';

        if (currentMeeting.audioUrl) {
            audioPlayer.src = currentMeeting.audioUrl;
        } else {
            audioPlayer.removeAttribute('src');
        }

        dropdown.classList.remove('open');
        closeAllPanels();
        audioPanel.classList.add('open');
    }

    function openSummary() {
        summaryTitle.textContent = currentMeeting.summaryTitle;
        summaryText.textContent = currentMeeting.summaryText;
        dropdown.classList.remove('open');
        closeAllPanels();
        summaryPanel.classList.add('open');
    }

    function openAssistantChat() {
        dropdown.classList.remove('open');
        closeAllPanels();
        assistantPanel.classList.add('open');
        setTimeout(function () {
            assistantTextarea.focus();
        }, 100);
    }

    function openMeetingChat() {
        meetingChatTitle.textContent = currentMeeting.title + ' - AI 질문';
        resetMeetingChat();
        dropdown.classList.remove('open');
        closeAllPanels();
        meetingChatPanel.classList.add('open');
        setTimeout(function () {
            meetingChatTextarea.focus();
        }, 100);
    }

    function resetMeetingChat() {
        meetingChatMessages.innerHTML =
            '<div class="vai-chat-empty">' +
            '  <svg viewBox="0 0 24 24" width="32" height="32" fill="rgb(139,152,165)">' +
            '    <path d="M20.93 11.94c0-4.6-3.95-8.42-8.93-8.42s-8.93 3.82-8.93 8.42c-.01.83.13 1.6.33 2.29.1.34.21.65.3.94.1.3.18.55.25.79.13.48.19.94.11 1.46-.08.6-.27 1.23-.58 1.91 1.3.39 2.62.06 4.12-.61l.47-.21.44.25c1.1.61 1.87 1.11 3.49 1.11 4.98 0 8.93-3.63 8.93-8.18z"></path>' +
            '  </svg>' +
            '  <span>이 녹음에 대해 질문해보세요</span>' +
            '</div>';
    }

    async function sendAssistantMessage() {
        const text = assistantTextarea.value.trim();
        if (!text || assistantRequestInFlight) {
            return;
        }

        assistantRequestInFlight = true;
        addMessage(assistantMessages, text, 'user');
        assistantTextarea.value = '';
        assistantTextarea.style.height = 'auto';
        setAssistantComposerDisabled(true);

        const loadingMessage = addMessage(
            assistantMessages,
            '답변을 준비하고 있습니다...',
            'ai',
            'vai-msg-loading'
        );

        try {
            const response = await fetch('/ai/chat/query', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ question: text })
            });

            if (!response.ok) {
                throw new Error('챗봇 응답을 불러오지 못했습니다.');
            }

            const payload = await response.json();
            loadingMessage.remove();
            addMessage(
                assistantMessages,
                payload && payload.answer ? payload.answer : '답변이 비어 있습니다.',
                'ai'
            );
        } catch (error) {
            loadingMessage.remove();
            addMessage(
                assistantMessages,
                '지금은 답변을 가져오지 못했습니다. 잠시 후 다시 시도해주세요.',
                'ai'
            );
            console.error(error);
        } finally {
            assistantRequestInFlight = false;
            setAssistantComposerDisabled(false);
            assistantTextarea.focus();
        }
    }

    function sendMeetingMessage() {
        const text = meetingChatTextarea.value.trim();
        if (!text) {
            return;
        }

        addMessage(meetingChatMessages, text, 'user');
        meetingChatTextarea.value = '';
        meetingChatTextarea.style.height = 'auto';

        setTimeout(function () {
            addMessage(meetingChatMessages, '녹음 내용을 분석하고 있습니다...', 'ai');
        }, 800);
    }

    fab.addEventListener('click', function (event) {
        event.stopPropagation();

        if (dropdown.classList.contains('open')) {
            closeEverything();
            return;
        }

        closeAllPanels();
        dropdown.classList.add('open');
    });

    assistantToggle.addEventListener('click', function (event) {
        event.stopPropagation();
        openAssistantChat();
    });

    meetingToggle.addEventListener('click', function (event) {
        event.stopPropagation();
        const isOpen = meetingList.classList.contains('open');

        if (isOpen) {
            meetingList.classList.remove('open');
            meetingChevron.classList.remove('expanded');
            collapseAllSubActions();
            return;
        }

        meetingList.classList.add('open');
        meetingChevron.classList.add('expanded');
    });

    assistantBack.addEventListener('click', function (event) {
        event.stopPropagation();
        backToDropdown();
    });

    audioBack.addEventListener('click', function (event) {
        event.stopPropagation();
        backToDropdown();
    });

    summaryBack.addEventListener('click', function (event) {
        event.stopPropagation();
        backToDropdown();
    });

    meetingChatBack.addEventListener('click', function (event) {
        event.stopPropagation();
        backToDropdown();
    });

    assistantClose.addEventListener('click', function (event) {
        event.stopPropagation();
        closeEverything();
    });

    audioClose.addEventListener('click', function (event) {
        event.stopPropagation();
        closeEverything();
    });

    summaryClose.addEventListener('click', function (event) {
        event.stopPropagation();
        closeEverything();
    });

    meetingChatClose.addEventListener('click', function (event) {
        event.stopPropagation();
        closeEverything();
    });

    document.addEventListener('click', function (event) {
        const container = document.getElementById('vaiContainer');
        if (!container.contains(event.target)) {
            closeEverything();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closeEverything();
        }
    });

    assistantSendBtn.addEventListener('click', function (event) {
        event.stopPropagation();
        sendAssistantMessage();
    });

    assistantTextarea.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            sendAssistantMessage();
        }
    });

    assistantTextarea.addEventListener('input', function () {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 100) + 'px';
    });

    meetingChatSendBtn.addEventListener('click', function (event) {
        event.stopPropagation();
        sendMeetingMessage();
    });

    meetingChatTextarea.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            sendMeetingMessage();
        }
    });

    meetingChatTextarea.addEventListener('input', function () {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 100) + 'px';
    });

    renderMeetings();
});
