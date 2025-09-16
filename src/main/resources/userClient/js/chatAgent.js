import { marked } from 'https://cdn.jsdelivr.net/npm/marked@4.3.0/lib/marked.esm.min.js';

// 配置 marked 的选项,处理换行
marked.setOptions({
    breaks: true // 将换行符 (\n) 转换为 <br> 标签
});

document.addEventListener("DOMContentLoaded", () => {
    const chatMessages = document.getElementById("chat-messages");
    const chatForm = document.getElementById("chat-form");
    const userInput = document.getElementById("user-input");
    const sendButton = document.getElementById("send-button");
    const chatStatus = document.getElementById("chat-status");

    // 等待Vue实例设置sessionId
    function waitForSessionId() {
        return new Promise((resolve) => {
            const checkSessionId = () => {
                if (window.chatSessionId) {
                    resolve(window.chatSessionId);
                } else {
                    setTimeout(checkSessionId, 100);
                }
            };
            checkSessionId();
        });
    }

    let sessionId = null;
    let userInfo = null;

    // 初始化
    waitForSessionId().then((sid) => {
        sessionId = sid;
        userInfo = window.userInfo;
        
        console.log('Agent会话已初始化:', { sessionId, userInfo });
        chatStatus.textContent = `欢迎 ${userInfo.userName || userInfo.nickname || '用户'}，导览僧为您服务`;
        
        // 添加初始欢迎消息
        addBotMessage("欢迎来到古董展览馆！我是您的智能导览僧，可以为您介绍展览信息、回答规则问题、推荐合适的展览。请问您想了解什么？");
    });

    // 消息历史记录
    const messageHistory = [];

    // 提交表单事件
    chatForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        
        if (!sessionId) {
            alert('会话未初始化，请刷新页面重试');
            return;
        }

        const message = userInput.value.trim();
        if (!message) return;

        // 记录用户消息
        messageHistory.push({
            role: "user",
            content: message,
            timestamp: new Date().toISOString()
        });

        // 显示用户消息
        addUserMessage(message);
        userInput.value = "";
        
        // 禁用输入和按钮
        userInput.disabled = true;
        sendButton.disabled = true;
        sendButton.textContent = "思考中...";
        chatStatus.textContent = "导览僧正在思考您的问题...";

        try {
            // 调用Agent API
            const response = await fetch('/api/guide-agent/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Authorization': `Bearer ${sessionStorage.getItem('token')}`
                },
                body: new URLSearchParams({
                    message: message,
                    sessionId: sessionId
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.text();
            
            // 记录助手回复
            messageHistory.push({
                role: "assistant",
                content: result,
                timestamp: new Date().toISOString()
            });

            // 显示助手回复
            addBotMessage(result);
            chatStatus.textContent = "回答完成，请继续提问";

        } catch (error) {
            console.error('Agent请求失败:', error);
            
            let errorMessage = "抱歉，导览僧暂时无法回答您的问题。";
            if (error.message.includes('401')) {
                errorMessage = "您的登录已过期，请重新登录。";
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
            } else if (error.message.includes('500')) {
                errorMessage = "服务器内部错误，请稍后再试。";
            } else if (error.message.includes('timeout')) {
                errorMessage = "请求超时，请稍后再试。";
            }
            
            addBotMessage(errorMessage);
            chatStatus.textContent = "发生错误，请重试";
        } finally {
            // 重新启用输入和按钮
            userInput.disabled = false;
            sendButton.disabled = false;
            sendButton.textContent = "发送";
            userInput.focus();
        }
    });

    // 添加用户消息到界面
    function addUserMessage(message) {
        const messageElement = document.createElement("div");
        messageElement.classList.add("message", "user-message");

        const messageText = document.createElement("div");
        messageText.classList.add("message-text");
        messageText.textContent = message;

        const avatarImg = document.createElement("img");
        avatarImg.alt = "User Avatar";
        avatarImg.classList.add("user-avatar");

        messageElement.appendChild(messageText);
        messageElement.appendChild(avatarImg);

        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // 添加机器人消息到界面
    function addBotMessage(message) {
        const messageElement = document.createElement("div");
        messageElement.classList.add("message", "bot-message");

        const avatarImg = document.createElement("img");
        avatarImg.src = "img/monkTang.svg";
        avatarImg.alt = "Bot Avatar";
        avatarImg.classList.add("bot-avatar");

        const messageText = document.createElement("div");
        messageText.classList.add("message-text");

        // 将 Markdown 转换为 HTML
        let htmlContent = marked.parse(message);

        // 手动处理换行符
        htmlContent = htmlContent.replace(/\n/g, '<br>');

        messageText.innerHTML = htmlContent;

        messageElement.appendChild(avatarImg);
        messageElement.appendChild(messageText);

        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // 添加错误消息处理
    function addErrorMessage(error) {
        const messageElement = document.createElement("div");
        messageElement.classList.add("message", "bot-message", "error-message");
        messageElement.style.borderLeft = "4px solid #ff6b6b";
        messageElement.style.backgroundColor = "#fff5f5";

        const avatarImg = document.createElement("img");
        avatarImg.src = "img/monkTang.svg";
        avatarImg.alt = "Bot Avatar";
        avatarImg.classList.add("bot-avatar");

        const messageText = document.createElement("div");
        messageText.classList.add("message-text");
        messageText.style.color = "#d63031";
        messageText.textContent = error;

        messageElement.appendChild(avatarImg);
        messageElement.appendChild(messageText);

        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // 键盘快捷键
    userInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            chatForm.dispatchEvent(new Event("submit"));
        }
    });

    // 导出一些方法到全局，便于调试
    window.AgentChat = {
        getSessionId: () => sessionId,
        getUserInfo: () => userInfo,
        getMessageHistory: () => messageHistory,
        clearMessages: () => {
            chatMessages.innerHTML = '';
            messageHistory.length = 0;
        }
    };
});
