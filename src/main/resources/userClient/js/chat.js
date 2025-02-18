document.addEventListener("DOMContentLoaded", () => {
    const chatMessages = document.getElementById("chat-messages")
    const chatForm = document.getElementById("chat-form")
    const userInput = document.getElementById("user-input")

    // 初始欢迎消息
    addBotMessage(
        "欢迎来到古董展览馆。我是您的导览僧，很高兴为您介绍我们珍贵的文物收藏。请问您对哪个朝代或者哪类文物最感兴趣？",
    )

    chatForm.addEventListener("submit", (e) => {
        e.preventDefault()
        const message = userInput.value.trim()
        if (message) {
            addUserMessage(message)
            userInput.value = ""
            // 模拟机器人回复
            setTimeout(() => {
                const botReply = getBotReply(message)
                addBotMessage(botReply)
            }, 1000)
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

    function getBotReply(message) {
        // 这里可以实现更复杂的回复逻辑
        const replies = [
            "这是一个很好的问题！这件文物来自于唐代，是我们馆藏的精品之一。",
            "您对这个感兴趣真是太好了！这件文物展示了古代工匠的高超技艺。",
            "非常感谢您的提问。这件文物不仅有很高的艺术价值，还反映了当时的社会生活。",
            "这个问题很有深度！这件文物的历史可以追溯到宋代，经历了漫长的岁月。",
            "您的观察很敏锐！这件文物的纹饰确实有很多有趣的细节，让我为您详细介绍一下。",
        ]
        return replies[Math.floor(Math.random() * replies.length)]
    }
})

