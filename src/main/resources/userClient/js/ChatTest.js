document.addEventListener("DOMContentLoaded", () => {
    const chatMessages = document.getElementById("chat-messages")
    const chatForm = document.getElementById("chat-form")
    const userInput = document.getElementById("user-input")

    // 存储对话历史
    const messageHistory = [{
        role: "assistant",
        content: "欢迎来到古董展览馆。我是您的导览僧，很高兴为您介绍我们珍贵的文物收藏。请问您对哪个朝代或者哪类文物最感兴趣？"
    }]

    // 初始欢迎消息
    addBotMessage(messageHistory[0].content)

    // 修改后的submit事件监听
    chatForm.addEventListener("submit", async (e) => {
        e.preventDefault()
        const message = userInput.value.trim()
        if (message) {
            messageHistory.push({
                role: "user",
                content: message
            })

            addUserMessage(message)
            userInput.value = ""
            userInput.disabled = true

            try {
                const response = await fetch("/api/chat/send", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json;charset=UTF-8",
                        "Accept": "text/event-stream;charset=UTF-8"  // 接受SSE
                    },
                    body: JSON.stringify({ userInput: message })  // 发送JSON
                })

                const reader = response.body.getReader()
                const decoder = new TextDecoder()
                let buffer = ""

                // 创建消息元素
                const messageElement = document.createElement("div")
                messageElement.classList.add("message", "bot-message")
                messageElement.id = "current-bot-message"

                const avatarImg = document.createElement("img")
                avatarImg.src = "monkTang.svg"
                avatarImg.alt = "Bot Avatar"
                avatarImg.classList.add("bot-avatar")

                const messageText = document.createElement("div")
                messageText.classList.add("message-text")
                messageText.textContent = ""

                messageElement.appendChild(avatarImg)
                messageElement.appendChild(messageText)
                chatMessages.appendChild(messageElement)

                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;

                    // 处理可能的chunk边界情况
                    buffer += decoder.decode(value, { stream: true });

                    // 当需要更新UI时（例如，当你想要显示部分结果时）
                    // 在这里处理buffer以移除多余的空格
                    let processedBuffer = buffer.replace(/\s+/g, ' ').trim();  // 使用正则表达式替换多个空白字符为单个空格，并修剪首尾空格

                    // 直接更新DOM
                    messageText.textContent = processedBuffer;
                    chatMessages.scrollTop = chatMessages.scrollHeight;
                }


                messageHistory.push({
                    role: "assistant",
                    content: buffer
                })
            } catch (error) {
                console.error("Error:", error)
                addBotMessage("抱歉，我遇到了一些问题。请稍后再试。")
            } finally {
                userInput.disabled = false
            }
        }
    })

    function addUserMessage(message) {
        const messageElement = document.createElement("div")
        messageElement.classList.add("message", "user-message")
        
        const messageText = document.createElement("div")
        messageText.classList.add("message-text")
        messageText.textContent = message
        
        const avatarImg = document.createElement("img")
        avatarImg.alt = "User Avatar"
        avatarImg.classList.add("user-avatar")
        
        messageElement.appendChild(messageText)
        messageElement.appendChild(avatarImg)
        
        chatMessages.appendChild(messageElement)
        chatMessages.scrollTop = chatMessages.scrollHeight
    }

    function addBotMessage(message) {
        const messageElement = document.createElement("div")
        messageElement.classList.add("message", "bot-message")

        const avatarImg = document.createElement("img")
        avatarImg.alt = "Bot Avatar"
        avatarImg.classList.add("bot-avatar")
        
        const messageText = document.createElement("div")
        messageText.classList.add("message-text")
        messageText.textContent = message

        messageElement.appendChild(avatarImg)
        messageElement.appendChild(messageText)

        chatMessages.appendChild(messageElement)
        chatMessages.scrollTop = chatMessages.scrollHeight
    }


})

